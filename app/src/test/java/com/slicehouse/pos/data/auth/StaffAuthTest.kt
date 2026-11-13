package com.slicehouse.pos.data.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.slicehouse.pos.data.db.PosDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sign-in rules against a real (in-memory) database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StaffAuthTest {

    private lateinit var db: PosDatabase
    private val scopes = mutableListOf<CoroutineScope>()
    private var now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        db.close()
    }

    /** A new StaffAuth on the same database, like the app restarting. */
    private fun open(): StaffAuth = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val auth = StaffAuth(db.staffDao(), scope) { now }
        withTimeout(10_000) { auth.state.first { it.loaded } }
        auth
    }

    /** Owner "Nasran" (PIN 1111) and cashier "Aina" (PIN 2222). */
    private fun withStaff(): StaffAuth = open().also { auth ->
        runBlocking {
            assertNull(auth.setUpOwner("Nasran", "1111"))
            assertNull(auth.addStaff("Aina", StaffRole.CASHIER, "2222"))
            auth.lock()
        }
    }

    private fun StaffAuth.id(name: String) = state.value.staff.first { it.name == name }.id

    @Test
    fun firstLaunchNeedsAnOwner() = runBlocking {
        val auth = open()
        assertTrue(auth.state.value.needsSetup)

        assertNull(auth.setUpOwner("Nasran", "1111"))
        val state = auth.state.value
        assertFalse(state.needsSetup)
        assertEquals("Nasran", state.signedIn?.name)
        assertTrue(state.signedIn!!.isOwner)
        assertEquals(12, auth.newRecoveryCode.value?.length)

        assertNotNull(auth.setUpOwner("Someone", "2222")) // only once
    }

    @Test
    fun badSetupAndDuplicateNamesAreRefused() = runBlocking {
        val auth = open()
        assertNotNull(auth.setUpOwner("", "1111"))
        assertNotNull(auth.setUpOwner("Nasran", "12"))
        assertNull(auth.setUpOwner("Nasran", "1111"))
        assertNotNull(auth.addStaff("nasran", StaffRole.CASHIER, "3333"))
    }

    @Test
    fun staffSignInWithTheirOwnPin() = runBlocking {
        val auth = withStaff()
        val aina = auth.id("Aina")

        assertEquals(PinResult.Wrong(4), auth.signIn(aina, "1111")) // the owner's PIN isn't Aina's
        val ok = auth.signIn(aina, "2222")
        assertTrue(ok is PinResult.Success)
        assertEquals("Aina", auth.state.value.signedIn?.name)

        auth.lock()
        assertNull(auth.state.value.signedIn)
    }

    @Test
    fun onlyAnOwnerPinApproves() = runBlocking {
        val auth = withStaff()
        auth.signIn(auth.id("Aina"), "2222")

        assertTrue(auth.approveAsOwner("2222") is PinResult.Wrong)
        val approved = auth.approveAsOwner("1111")
        assertEquals("Nasran", (approved as PinResult.Success).staff.name)
        assertEquals("Aina", auth.state.value.signedIn?.name) // approval doesn't switch user
    }

    @Test
    fun fiveWrongTriesLockForThirtySecondsEvenAfterRestart() = runBlocking {
        val auth = withStaff()
        val aina = auth.id("Aina")
        repeat(4) { i -> assertEquals(PinResult.Wrong(4 - i), auth.signIn(aina, "9999")) }
        val locked = auth.signIn(aina, "9999")
        assertEquals(PinResult.LockedOut(now + StaffAuth.LOCKOUT_MILLIS), locked)

        // Right PIN is still refused while locked, also after restarting the app.
        assertTrue(auth.signIn(aina, "2222") is PinResult.LockedOut)
        val restarted = open()
        assertTrue(restarted.signIn(aina, "2222") is PinResult.LockedOut)

        now += StaffAuth.LOCKOUT_MILLIS
        assertTrue(restarted.signIn(aina, "2222") is PinResult.Success)
        assertEquals(PinResult.Wrong(4), restarted.signIn(aina, "9999")) // counter starts fresh
    }

    @Test
    fun lastOwnerCantBeTurnedOff() = runBlocking {
        val auth = withStaff()
        val nasran = auth.id("Nasran")
        assertNotNull(auth.setActive(nasran, false))

        assertNull(auth.setActive(auth.id("Aina"), false))
        assertFalse(auth.state.value.activeStaff.any { it.name == "Aina" })
        assertTrue(auth.signIn(auth.id("Aina"), "2222") is PinResult.Wrong) // turned-off accounts can't sign in
    }

    @Test
    fun recoveryCodeResetsTheOwnerPinOnce() = runBlocking {
        val auth = open()
        auth.setUpOwner("Nasran", "1111")
        val code = auth.newRecoveryCode.value!!
        auth.recoveryCodeShown()
        auth.lock()

        assertTrue(auth.recoverOwner("WRONGCODE123", "5555") is PinResult.Wrong)
        val typed = PinHasher.formatRecoveryCode(code).lowercase() // how a person might type it
        assertTrue(auth.recoverOwner(typed, "5555") is PinResult.Success)
        assertEquals("Nasran", auth.state.value.signedIn?.name)
        assertNotNull(auth.newRecoveryCode.value) // a fresh code to write down

        val nasran = auth.id("Nasran")
        auth.lock()
        assertTrue(auth.signIn(nasran, "1111") is PinResult.Wrong)
        assertTrue(auth.signIn(nasran, "5555") is PinResult.Success)
        assertTrue(auth.recoverOwner(code, "6666") is PinResult.Wrong) // old code is used up
    }

    @Test
    fun changedPinWorksAndOldOneDoesnt() = runBlocking {
        val auth = withStaff()
        val aina = auth.id("Aina")
        assertNull(auth.changePin(aina, "3333"))
        assertNotNull(auth.changePin(aina, "12"))
        assertTrue(auth.signIn(aina, "2222") is PinResult.Wrong)
        assertTrue(auth.signIn(aina, "3333") is PinResult.Success)
    }
}
