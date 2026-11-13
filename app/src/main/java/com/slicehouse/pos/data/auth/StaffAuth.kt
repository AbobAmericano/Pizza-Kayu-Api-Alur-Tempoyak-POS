package com.slicehouse.pos.data.auth

import com.slicehouse.pos.data.db.MetaEntity
import com.slicehouse.pos.data.db.StaffDao
import com.slicehouse.pos.data.db.StaffDao.Companion.KEY_FAILED_PINS
import com.slicehouse.pos.data.db.StaffDao.Companion.KEY_LOCKED_UNTIL
import com.slicehouse.pos.data.db.StaffDao.Companion.KEY_RECOVERY_HASH
import com.slicehouse.pos.data.db.StaffDao.Companion.KEY_RECOVERY_OWNER
import com.slicehouse.pos.data.db.StaffEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Staff accounts, signing in and owner approval.
 *
 * Rules:
 * - The first account is the owner, created at setup with a recovery code.
 * - Every PIN or recovery-code check counts toward one shared limit:
 *   after [MAX_ATTEMPTS] wrong tries, entry is blocked for [LOCKOUT_MILLIS].
 *   The counter is saved, so restarting the app doesn't reset it.
 * - There must always be at least one active owner.
 */
class StaffAuth(
    private val dao: StaffDao,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** The recovery code to show once, after setup or recovery. Never saved. */
    private val _newRecoveryCode = MutableStateFlow<String?>(null)
    val newRecoveryCode: StateFlow<String?> = _newRecoveryCode.asStateFlow()

    /** One PIN check or account change at a time, so the attempt counter can't be raced. */
    private val mutex = Mutex()

    init {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                refresh()
                val lockedUntil = dao.meta(KEY_LOCKED_UNTIL)?.toLongOrNull() ?: 0
                _state.update { it.copy(loaded = true, lockedUntil = lockedUntil) }
            }
        }
    }

    // ------------------------------------------------------------- signing in

    /** Creates the first owner and signs them in. Shows a new recovery code. */
    suspend fun setUpOwner(name: String, pin: String): String? = locked {
        if (_state.value.activeStaff.any { it.isOwner }) return@locked "An owner already exists."
        validate(name, pin)?.let { return@locked it }
        val owner = newStaff(name, StaffRole.OWNER, pin)
        dao.upsert(owner)
        issueRecoveryCode(owner.id)
        refresh()
        _state.update { it.copy(signedIn = owner.toModel()) }
        null
    }

    suspend fun signIn(staffId: String, pin: String): PinResult = locked {
        val result = checkSecret(pin) { candidates -> candidates.filter { it.id == staffId && it.active } }
        if (result is PinResult.Success) _state.update { it.copy(signedIn = result.staff) }
        result
    }

    /** Checks an owner PIN to approve one action. Doesn't change who is signed in. */
    suspend fun approveAsOwner(pin: String): PinResult = locked {
        checkSecret(pin) { candidates -> candidates.filter { it.active && it.role == StaffRole.OWNER } }
    }

    fun lock() {
        _state.update { it.copy(signedIn = null) }
    }

    /**
     * Forgotten owner PIN: the recovery code from setup sets a new PIN for that
     * owner and signs them in. The code is used up and a new one is shown.
     */
    suspend fun recoverOwner(code: String, newPin: String): PinResult = locked {
        if (!PinHasher.isValidPin(newPin)) return@locked PinResult.Wrong(attemptsLeft())
        val ownerId = dao.meta(KEY_RECOVERY_OWNER)
        val recoveryHash = dao.meta(KEY_RECOVERY_HASH).orEmpty()
        val normalized = PinHasher.normalizeRecoveryCode(code)
        val result = checkSecret(normalized, matches = { secret, _ -> PinHasher.verify(secret, recoveryHash) }) { all ->
            all.filter { it.id == ownerId }
        }
        if (result is PinResult.Success) {
            dao.setPin(result.staff.id, PinHasher.hash(newPin))
            dao.setActive(result.staff.id, true)
            issueRecoveryCode(result.staff.id)
            refresh()
            _state.update { it.copy(signedIn = result.staff.copy(active = true)) }
        }
        result
    }

    /** Clears the recovery code from the screen once the owner has written it down. */
    fun recoveryCodeShown() {
        _newRecoveryCode.value = null
    }

    // ------------------------------------------------------- managing staff

    /** Returns an error message, or null when the account was added. */
    suspend fun addStaff(name: String, role: StaffRole, pin: String): String? = locked {
        validate(name, pin)?.let { return@locked it }
        dao.upsert(newStaff(name, role, pin))
        refresh()
        null
    }

    suspend fun changePin(staffId: String, pin: String): String? = locked {
        if (!PinHasher.isValidPin(pin)) return@locked "PIN must be 4 to 6 digits."
        dao.setPin(staffId, PinHasher.hash(pin))
        null
    }

    /** Turns an account on or off. The last active owner can't be turned off. */
    suspend fun setActive(staffId: String, active: Boolean): String? = locked {
        val target = _state.value.staff.firstOrNull { it.id == staffId } ?: return@locked "Account not found."
        if (!active && target.isOwner && _state.value.activeStaff.count { it.isOwner } <= 1) {
            return@locked "Add another owner before turning off the last one."
        }
        dao.setActive(staffId, active)
        refresh()
        if (!active && _state.value.signedIn?.id == staffId) lock()
        null
    }

    /** A fresh recovery code for the setup owner (the old one stops working). */
    suspend fun regenerateRecoveryCode() = locked {
        val ownerId = dao.meta(KEY_RECOVERY_OWNER) ?: _state.value.activeStaff.first { it.isOwner }.id
        issueRecoveryCode(ownerId)
    }

    // ------------------------------------------------------------- internals

    private suspend fun <T> locked(block: suspend () -> T): T =
        mutex.withLock { withContext(Dispatchers.Default) { block() } }

    private suspend fun refresh() {
        val staff = dao.all().map { it.toModel() }.sortedBy { if (it.isOwner) 0 else 1 }
        _state.update { current ->
            current.copy(
                staff = staff,
                signedIn = current.signedIn?.let { me -> staff.firstOrNull { it.id == me.id && it.active } }
            )
        }
    }

    /**
     * Compares [secret] with the hashes of [pick]ed accounts, applying the
     * shared wrong-attempt limit.
     */
    private suspend fun checkSecret(
        secret: String,
        matches: (String, StaffEntity) -> Boolean = { s, staff -> PinHasher.verify(s, staff.pinHash) },
        pick: (List<StaffEntity>) -> List<StaffEntity>
    ): PinResult {
        val now = clock()
        val lockedUntil = dao.meta(KEY_LOCKED_UNTIL)?.toLongOrNull() ?: 0
        if (now < lockedUntil) return PinResult.LockedOut(lockedUntil)

        val hit = pick(dao.all()).firstOrNull { matches(secret, it) }
        if (hit != null) {
            dao.putMeta(MetaEntity(KEY_FAILED_PINS, "0"))
            return PinResult.Success(hit.toModel())
        }

        val failed = (dao.meta(KEY_FAILED_PINS)?.toIntOrNull() ?: 0) + 1
        if (failed >= MAX_ATTEMPTS) {
            val until = now + LOCKOUT_MILLIS
            dao.putMeta(MetaEntity(KEY_LOCKED_UNTIL, until.toString()))
            dao.putMeta(MetaEntity(KEY_FAILED_PINS, "0"))
            _state.update { it.copy(lockedUntil = until) }
            return PinResult.LockedOut(until)
        }
        dao.putMeta(MetaEntity(KEY_FAILED_PINS, failed.toString()))
        return PinResult.Wrong(MAX_ATTEMPTS - failed)
    }

    private suspend fun attemptsLeft(): Int =
        MAX_ATTEMPTS - (dao.meta(KEY_FAILED_PINS)?.toIntOrNull() ?: 0)

    private suspend fun issueRecoveryCode(ownerId: String) {
        val code = PinHasher.newRecoveryCode()
        dao.putMeta(MetaEntity(KEY_RECOVERY_HASH, PinHasher.hash(code)))
        dao.putMeta(MetaEntity(KEY_RECOVERY_OWNER, ownerId))
        _newRecoveryCode.value = code
    }

    private fun validate(name: String, pin: String): String? = when {
        name.isBlank() -> "Enter a name."
        _state.value.activeStaff.any { it.name.equals(name.trim(), ignoreCase = true) } ->
            "Someone called ${name.trim()} already exists."
        !PinHasher.isValidPin(pin) -> "PIN must be 4 to 6 digits."
        else -> null
    }

    private fun newStaff(name: String, role: StaffRole, pin: String) = StaffEntity(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        role = role,
        pinHash = PinHasher.hash(pin),
        active = true,
        createdAt = clock()
    )

    private fun StaffEntity.toModel() = Staff(id = id, name = name, role = role, active = active)

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L
    }
}
