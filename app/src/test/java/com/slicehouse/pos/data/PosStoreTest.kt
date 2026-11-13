package com.slicehouse.pos.data

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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Runs PosStore against a real (in-memory) Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PosStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: PosDatabase
    private lateinit var filesDir: File
    private val scopes = mutableListOf<CoroutineScope>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        filesDir = folder.newFolder("files")
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        db.close()
    }

    /** A new store on the same database, like the app restarting. */
    private fun openStore(): PosStore = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val store = PosStore(db.dao(), filesDir, scope)
        withTimeout(10_000) { store.state.first { it.loaded } }
        store
    }

    private fun PosStore.flush() = runBlocking { withTimeout(10_000) { awaitWrites() } }

    @Test
    fun freshInstallStartsWithTheStarterMenu() {
        val state = openStore().state.value
        assertEquals(SampleData.menu, state.menu)
        assertEquals(SampleData.toppings, state.toppings)
        assertEquals(PosState.FIRST_ORDER_NUMBER, state.nextOrderNumber)
        assertNull(state.storageWarning)
    }

    @Test
    fun paidOrderSurvivesARestart() {
        val store = openStore()
        store.addLine(CartLine("", "p1", "4in1", "P", unitPrice = 2300, qty = 2, toppings = listOf("Extra cheese")))
        store.setPickupTime("19:30")
        val order = store.completeOrder(PaymentMethod.CASH, cashReceived = 6000)!!
        store.flush()

        val reopened = openStore().state.value
        val saved = reopened.orders.single()
        assertEquals(order, saved)
        assertEquals(PosState.FIRST_ORDER_NUMBER + 1, reopened.nextOrderNumber)
        assertTrue(reopened.cart.isEmpty())
    }

    @Test
    fun refundAndMenuChangesArePersisted() {
        val store = openStore()
        store.addLine(CartLine("", "d1", "Coca-Cola", "C", unitPrice = 250, qty = 1))
        val order = store.completeOrder(PaymentMethod.CARD, 0, takenBy = "Aina")!!
        store.refund(order.id, approvedBy = "Nasran")
        store.saveMenuItem(SampleData.menu.first().copy(price = 2200))
        store.deleteMenuItem("x4")
        store.updateSettings(store.state.value.settings.copy(taxBasisPoints = 600))
        store.flush()

        val reopened = openStore().state.value
        assertEquals(OrderStatus.REFUNDED, reopened.orders.single().status)
        assertNotNull(reopened.orders.single().refundedAt)
        assertEquals("Aina", reopened.orders.single().takenBy)
        assertEquals("Nasran", reopened.orders.single().refundedBy)
        assertEquals(2200L, reopened.menu.first().price)
        assertFalse(reopened.menu.any { it.id == "x4" })
        assertEquals(SampleData.menu.map { it.id } - "x4", reopened.menu.map { it.id })
        assertEquals(600, reopened.settings.taxBasisPoints)
    }

    @Test
    fun resetDeletesOrdersButKeepsSettings() {
        val store = openStore()
        store.updateSettings(store.state.value.settings.copy(shopName = "Test shop"))
        store.addLine(CartLine("", "d1", "Coca-Cola", "C", unitPrice = 250, qty = 1))
        store.completeOrder(PaymentMethod.CARD, 0)
        store.resetAll()
        store.flush()

        val reopened = openStore().state.value
        assertTrue(reopened.orders.isEmpty())
        assertEquals(PosState.FIRST_ORDER_NUMBER, reopened.nextOrderNumber)
        assertEquals("Test shop", reopened.settings.shopName)
    }

    @Test
    fun importsTheOldJsonFileOnceAndKeepsACopy() {
        File(filesDir, LegacyImport.FILE_NAME).writeText(LegacySamples.V14_FILE)

        val state = openStore().state.value
        assertEquals(1004, state.orders.single().number)
        assertEquals(4899L, state.orders.single().total)
        assertEquals(1005, state.nextOrderNumber)
        assertEquals(650, state.settings.taxBasisPoints)
        assertNull(state.storageWarning)
        assertFalse(File(filesDir, LegacyImport.FILE_NAME).exists())
        assertTrue(File(filesDir, LegacyImport.IMPORTED_FILE_NAME).exists())

        // A second launch reads the database and doesn't import again.
        assertEquals(1, openStore().state.value.orders.size)
    }

    @Test
    fun unreadableOldFileIsKeptAndTheUserIsTold() {
        val legacy = File(filesDir, LegacyImport.FILE_NAME)
        legacy.writeText("{ half written fil")

        val store = openStore()
        assertNotNull(store.state.value.storageWarning)
        assertEquals(SampleData.menu, store.state.value.menu)
        assertEquals("{ half written fil", legacy.readText())

        store.dismissStorageWarning()
        store.flush()
        assertNull(openStore().state.value.storageWarning)
        assertTrue(legacy.exists())
    }
}
