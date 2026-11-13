package com.slicehouse.pos.data.auth

enum class StaffRole(val label: String) {
    OWNER("Owner"),
    CASHIER("Cashier")
}

/** A person who can sign in to the register. PINs are never kept here, only in the database as hashes. */
data class Staff(
    val id: String,
    val name: String,
    val role: StaffRole,
    val active: Boolean = true
) {
    val isOwner: Boolean get() = role == StaffRole.OWNER
}

/** What happened when someone typed a PIN or recovery code. */
sealed interface PinResult {
    data class Success(val staff: Staff) : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val until: Long) : PinResult
}

/** Everything the screens need to know about who is signed in. */
data class AuthState(
    /** False until staff accounts have been read from the database. */
    val loaded: Boolean = false,
    /** All accounts, owners first, including deactivated ones. */
    val staff: List<Staff> = emptyList(),
    val signedIn: Staff? = null,
    /** PIN entry is blocked until this time (epoch millis) after too many wrong tries. */
    val lockedUntil: Long = 0
) {
    val activeStaff: List<Staff> get() = staff.filter { it.active }

    /** No active owner yet: first launch, or upgrading from a version without PINs. */
    val needsSetup: Boolean get() = loaded && activeStaff.none { it.isOwner }
}
