package com.slicehouse.pos.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.MenuChange
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.SampleData
import com.slicehouse.pos.data.db.PosDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
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
import java.util.UUID

/** The tablet's sync against [FakeFireLord], with a real (in-memory) Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val fake = FakeFireLord()
    private val server = MockWebServer()
    private lateinit var db: PosDatabase
    private lateinit var store: PosStore
    private lateinit var sync: SyncManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val url get() = server.url("/").toString()

    @Before
    fun setUp() {
        server.dispatcher = fake
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        store = PosStore(db.dao(), folder.newFolder("files"), scope)
        runBlocking { withTimeout(10_000) { store.state.first { it.loaded } } }
        sync = newSync()
        store.menuMaster = sync
    }

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
        db.close()
    }

    private fun newSync(allowHttp: Boolean = true) =
        SyncManager(db.dao(), store, scope, allowHttp = allowHttp, deviceName = "Test tablet")

    private fun connect(): String? = runBlocking { withTimeout(20_000) { sync.connect(url, fake.code) } }

    private fun syncNow(): Boolean = runBlocking { withTimeout(20_000) { sync.syncNow() } }

    private fun sale(price: Long = 2300): Order {
        store.addLine(CartLine("", "p1", "4in1", "P", unitPrice = price, qty = 1))
        return store.completeOrder(PaymentMethod.CASH, 10_000, takenBy = "Aina")!!
    }

    private fun waitUntil(what: String, condition: () -> Boolean) = runBlocking {
        withTimeout(10_000) {
            while (!condition()) delay(20)
        }
        assertTrue(what, condition())
    }

    private fun postsTo(path: String) = fake.requests.count { it == "POST $path" }

    // ------------------------------------------------------------- connecting

    @Test
    fun firstTabletSendsItsMenuUp() {
        assertNull(connect())

        assertEquals(1L, fake.version)
        assertEquals(SampleData.menu.map { it.id }, fake.items.map { it.id })
        assertEquals(SampleData.toppings.size, fake.toppings.size)
        val status = sync.status.value
        assertTrue(status.connected)
        assertEquals("Pizza Kayu Api", status.shopName)
        assertNotNull(status.lastSyncAt)
    }

    @Test
    fun fireLordsMenuAndTaxReplaceTheTablets() {
        fake.items = listOf(MenuItemDto("w1", "Daebak Chicken", category = "PIZZA", priceSen = 2800, isPizza = true))
        fake.version = 5
        fake.tax = 600

        assertNull(connect())

        val state = store.state.value
        assertEquals(listOf("Daebak Chicken"), state.menu.map { it.name })
        assertEquals(2800L, state.menu.single().price)
        assertEquals(600, state.settings.taxBasisPoints)
        assertEquals(5L, fake.version) // nothing was uploaded over it
    }

    @Test
    fun wrongCodeIsExplained() {
        val problem = runBlocking { sync.connect(url, "000000") }
        assertTrue(problem!!, problem.contains("wrong or has expired"))
        assertFalse(sync.status.value.connected)
    }

    @Test
    fun releaseBuildsNeedHttps() {
        val release = newSync(allowHttp = false)
        val problem = runBlocking { release.connect(url, fake.code) }
        assertTrue(problem!!, problem.contains("https://"))
        assertTrue(fake.requests.isEmpty())
    }

    @Test
    fun connectionSurvivesARestart() {
        connect()
        val restarted = newSync()
        runBlocking { withTimeout(10_000) { restarted.status.first { it.loaded } } }
        assertTrue(restarted.isConnected)
        assertEquals("Pizza Kayu Api", restarted.status.value.shopName)
    }

    // ----------------------------------------------------------------- orders

    @Test
    fun ordersAndRefundsGoUpOnce() {
        val first = sale()
        val second = sale()
        store.refund(first.id, approvedBy = "Nasran") // refunded before it was ever uploaded

        connect()

        assertEquals(2, fake.orders.size)
        val sent = fake.orders.getValue(first.id)
        assertEquals("REFUNDED", sent.status)
        assertEquals("Nasran", sent.refundedBy)
        assertEquals("Aina", sent.takenBy)
        assertEquals(2484L, fake.orders.getValue(second.id).totalSen) // 23.00 + 8% tax
        assertTrue(fake.refunds.isEmpty()) // the refund travelled with the order

        store.refund(second.id, approvedBy = "Nasran")
        syncNow()
        assertEquals("Nasran", fake.refunds.getValue(second.id).refundedBy)

        val posts = postsTo("orders")
        syncNow()
        assertEquals("nothing is sent twice", posts, postsTo("orders"))
        waitUntil("nothing left waiting") { sync.status.value.waiting == 0 }
    }

    @Test
    fun wholeHistoryGoesUpInBatchesOf200() {
        repeat(250) { sale() }
        runBlocking { store.flush() }

        connect()

        assertEquals(250, fake.orders.size)
        assertEquals(2, postsTo("orders"))
    }

    @Test
    fun ordersTheServerRefusesAreNotRetried() {
        val good = sale()
        val unreadable = sale()
        val wrongTotals = sale()
        fake.invalidNumbers += unreadable.number
        fake.rejectNumbers += wrongTotals.number

        connect()

        assertEquals(listOf(good.id), fake.orders.keys.toList())
        waitUntil("both counted as refused") { sync.status.value.rejected == 2 }
        val posts = postsTo("orders")
        syncNow()
        assertEquals(posts, postsTo("orders"))
    }

    @Test
    fun offlineOrdersWaitForTheInternet() {
        connect()
        fake.offline = true
        val order = sale()

        assertFalse(syncNow())
        assertTrue(sync.status.value.error!!.contains("No internet"))
        waitUntil("the order is waiting") { sync.status.value.waiting == 1 }

        fake.offline = false
        assertTrue(syncNow())
        assertTrue(order.id in fake.orders)
        assertNull(sync.status.value.error)
    }

    @Test
    fun disconnectedOnFireLordStopsTheTablet() {
        connect()
        fake.revoked = true
        sale()

        assertFalse(syncNow())

        val status = sync.status.value
        assertFalse(status.connected)
        assertTrue(status.revoked)
        assertFalse(sync.isConnected)
        assertNull(runBlocking { db.dao().meta(SyncManager.KEY_DEVICE_KEY) })
        assertTrue("reset works again once not connected", store.resetAll())
    }

    // ------------------------------------------------------------------- menu

    @Test
    fun soldOutWorksOfflineAndIsSentLater() {
        connect()
        val id = SampleData.menu.first().id
        fake.offline = true

        store.toggleAvailable(id)

        assertFalse(store.state.value.menu.first { it.id == id }.available)
        fake.offline = false
        syncNow()
        waitUntil("FireLord has it sold out") { fake.items.firstOrNull { it.id == id }?.available == false }
    }

    @Test
    fun menuEditsNeedTheInternetWhileConnected() {
        connect()
        val item = store.state.value.menu.first()
        fake.offline = true

        val offline = runBlocking { store.saveMenuItem(item.copy(price = 9900)) }

        assertEquals(MenuChange.Offline, offline)
        assertEquals(item.price, store.state.value.menu.first().price)

        fake.offline = false
        val online = runBlocking { store.saveMenuItem(item.copy(price = 9900)) }
        assertEquals(MenuChange.Saved, online)
        assertEquals(9900L, store.state.value.menu.first().price)
        assertEquals(9900L, fake.items.first().priceSen)
    }

    @Test
    fun aChangeOnTheWebWinsOverAStaleEdit() {
        connect()
        fake.editOnWeb { items -> items.map { if (it.id == items.first().id) it.copy(name = "Renamed on web") else it } }
        val stale = store.state.value.menu.last()

        val result = runBlocking { store.saveMenuItem(stale.copy(price = 1)) }

        assertEquals(MenuChange.Conflict, result)
        assertEquals("Renamed on web", store.state.value.menu.first().name)
        assertEquals(stale.price, store.state.value.menu.last().price)
    }

    @Test
    fun webMenuChangesComeDownOnTheNextSync() {
        connect()
        val count = store.state.value.menu.size
        fake.editOnWeb { it.dropLast(1) }
        fake.tax = 600

        syncNow()

        assertEquals(count - 1, store.state.value.menu.size)
        assertEquals(600, store.state.value.settings.taxBasisPoints)
    }

    @Test
    fun taxCantBeChangedOnTheTabletWhileConnected() {
        connect()
        store.updateSettings(store.state.value.settings.copy(taxBasisPoints = 1000, footer = "Hi"))
        assertEquals(800, store.state.value.settings.taxBasisPoints)
        assertEquals("Hi", store.state.value.settings.footer)
        assertFalse("reset needs disconnecting first", store.resetAll())
    }

    @Test
    fun afterDisconnectTheTabletWorksOnItsOwn() {
        connect()
        runBlocking { sync.disconnect() }

        assertFalse(sync.status.value.connected)
        fake.offline = true
        val item = store.state.value.menu.first()
        assertEquals(MenuChange.Saved, runBlocking { store.saveMenuItem(item.copy(price = 1234)) })
        assertEquals(1234L, store.state.value.menu.first().price)
    }

    // ---------------------------------------------------------------- helpers

    @Test
    fun oldOrderIdsGetAStableUuid() {
        val id = UUID.randomUUID().toString()
        assertEquals(id, serverOrderId(id))
        val legacy = serverOrderId("o-17")
        assertEquals(legacy, serverOrderId("o-17"))
        assertEquals(legacy, UUID.fromString(legacy).toString())
    }

    @Test
    fun serverAddressIsCleanedUp() {
        assertEquals("https://firelord.example.com/", PosApi.parseServer("firelord.example.com", false).toString())
        assertEquals("https://firelord.example.com/", PosApi.parseServer(" https://firelord.example.com/pos/ ", false).toString())
        assertNull(PosApi.parseServer("http://10.0.2.2:8000", allowHttp = false))
        assertEquals("http://10.0.2.2:8000/", PosApi.parseServer("http://10.0.2.2:8000", allowHttp = true).toString())
        assertNull(PosApi.parseServer("", true))
    }
}
