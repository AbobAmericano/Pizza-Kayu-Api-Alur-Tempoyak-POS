package com.slicehouse.pos.data.sync

import com.slicehouse.pos.data.MenuChange
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.MenuMaster
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Topping
import com.slicehouse.pos.data.db.MetaEntity
import com.slicehouse.pos.data.db.OrderEntity
import com.slicehouse.pos.data.db.PosDao
import com.slicehouse.pos.data.db.SyncState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.atomic.AtomicBoolean

/** The Online screen's list. [error] is set when the last refresh failed. */
data class OnlineOrders(
    val orders: List<OnlineOrderDto> = emptyList(),
    val loaded: Boolean = false,
    val error: String? = null
) {
    val waiting: Int get() = orders.count { it.status == "PENDING" }
}

/** What the Settings screen shows about the FireLord connection. */
data class SyncStatus(
    val loaded: Boolean = false,
    val connected: Boolean = false,
    /** The server address, kept after disconnecting so reconnecting is quicker. */
    val server: String = "",
    val shopName: String = "",
    val lastSyncAt: Long? = null,
    val syncing: Boolean = false,
    /** Orders and refunds not uploaded yet. */
    val waiting: Int = 0,
    /** Orders the server refused (e.g. old orders whose totals don't add up). */
    val rejected: Int = 0,
    val error: String? = null,
    /** The owner disconnected this tablet on FireLord. */
    val revoked: Boolean = false
)

/**
 * Keeps the tablet and FireLord in step:
 *
 * - Orders and refunds go up (each order once; re-sending is harmless because
 *   the server knows orders by their id).
 * - The menu comes down whenever it changed on the server, and the tablet's
 *   tax rate follows the server's.
 * - Menu edits on the tablet go to the server first ([publish]); only sold-out
 *   switches work offline and are sent on the next sync.
 *
 * The very first sync after connecting uploads the tablet's menu if the server
 * has none yet; otherwise the server's menu replaces the tablet's.
 *
 * Everything that talks to the server runs one at a time (a [Mutex]).
 */
class SyncManager(
    private val dao: PosDao,
    private val store: PosStore,
    private val scope: CoroutineScope,
    private val api: PosApi = PosApi(),
    private val allowHttp: Boolean = false,
    private val deviceName: String = "Tablet",
    private val clock: () -> Long = System::currentTimeMillis
) : MenuMaster {

    private data class Connection(val server: HttpUrl, val key: String)

    @Volatile
    private var connection: Connection? = null

    /** The server's menu version this tablet has. [FIRST_MENU] = not synced yet. Guarded by [mutex]. */
    private var menuVersion = FIRST_MENU

    /** Sold-out switches made on the tablet and not sent yet: item id -> available. Guarded by [mutex]. */
    private val pendingAvailability = mutableMapOf<String, Boolean>()

    private val mutex = Mutex()
    private val rerun = AtomicBoolean(false)
    private val ready = CompletableDeferred<Unit>()
    private val json = Json
    private val availabilitySerializer = MapSerializer(String.serializer(), Boolean.serializer())

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _onlineOrders = MutableStateFlow(OnlineOrders())

    /** Online orders that still need the shop, refreshed every few seconds while the app is open. */
    val onlineOrders: StateFlow<OnlineOrders> = _onlineOrders.asStateFlow()

    /** Called when a new order is waiting to be accepted (the app plays a sound). */
    var onNewOnlineOrder: () -> Unit = {}
    private var waitingSeen: Set<String>? = null
    private val onlineMutex = Mutex()

    init {
        scope.launch {
            runCatching { load() }
            _status.update { it.copy(loaded = true) }
            ready.complete(Unit)
        }
        scope.launch { dao.waitingToUpload().collect { n -> _status.update { it.copy(waiting = n) } } }
        scope.launch { dao.rejectedUploads().collect { n -> _status.update { it.copy(rejected = n) } } }
    }

    // ---------------------------------------------------------------- connect

    /**
     * Pairs this tablet using the 6-digit code from FireLord (Pizza POS -> Tablets),
     * then runs the first sync. Returns null on success, or what went wrong.
     */
    suspend fun connect(serverInput: String, code: String): String? {
        ready.await()
        val server = PosApi.parseServer(serverInput, allowHttp)
            ?: return if (allowHttp) "Type the FireLord address, e.g. https://firelord.example.com or http://10.0.2.2:8000"
            else "Type the FireLord address, starting with https://"
        val digits = code.filter { it.isDigit() }
        if (digits.length != 6) return "The pairing code has 6 digits."

        mutex.withLock {
            val pairing = try {
                api.pair(server, digits, deviceName)
            } catch (e: ApiException) {
                return when (e) {
                    is ApiException.Http -> if (e.code == 400) {
                        "That code is wrong or has expired. Get a new one on FireLord."
                    } else {
                        "FireLord couldn't pair this tablet (${e.message})."
                    }
                    is ApiException.Offline -> "Can't reach $server. Check the address and the internet."
                    else -> e.message ?: "Couldn't connect."
                }
            }
            // The key is shown only once, so save it before anything else can fail.
            connection = Connection(server, pairing.deviceKey)
            menuVersion = FIRST_MENU
            pendingAvailability.clear()
            dao.putMeta(MetaEntity(KEY_SERVER, server.toString()))
            dao.putMeta(MetaEntity(KEY_DEVICE_KEY, pairing.deviceKey))
            dao.putMeta(MetaEntity(KEY_SHOP, pairing.shopName))
            dao.putMeta(MetaEntity(KEY_MENU_VERSION, FIRST_MENU.toString()))
            savePendingAvailability()
            _status.update {
                it.copy(
                    connected = true, revoked = false, server = server.toString(),
                    shopName = pairing.shopName, error = null
                )
            }
        }
        syncNow()
        return null
    }

    /** Stops syncing. Orders and the menu stay on the tablet. */
    suspend fun disconnect() {
        ready.await()
        mutex.withLock {
            connection = null
            menuVersion = FIRST_MENU
            pendingAvailability.clear()
            listOf(KEY_DEVICE_KEY, KEY_SHOP, KEY_MENU_VERSION, KEY_PENDING_AVAILABILITY).forEach { dao.deleteMeta(it) }
            _status.update { it.copy(connected = false, shopName = "", error = null, revoked = false) }
            _onlineOrders.value = OnlineOrders()
            waitingSeen = null
        }
    }

    // ------------------------------------------------------------------- sync

    /** Starts a sync in the background, e.g. right after a sale. */
    fun requestSync() {
        scope.launch { syncNow() }
    }

    /** Syncs every [intervalMillis] while connected. Runs until cancelled (the app goes to the background). */
    suspend fun runEvery(intervalMillis: Long = SYNC_INTERVAL_MILLIS) {
        while (true) {
            if (connection != null) syncNow()
            delay(intervalMillis)
        }
    }

    /**
     * One full sync. Returns true if everything went through. If a sync is
     * already running, this one is folded into it (it runs again at the end).
     */
    suspend fun syncNow(): Boolean {
        ready.await()
        if (connection == null) return false
        if (!mutex.tryLock()) {
            rerun.set(true)
            return false
        }
        var ok: Boolean
        try {
            _status.update { it.copy(syncing = true) }
            do {
                rerun.set(false)
                ok = syncPass()
            } while (rerun.get() && connection != null)
        } finally {
            _status.update { it.copy(syncing = false) }
            mutex.unlock()
        }
        return ok
    }

    private suspend fun syncPass(): Boolean {
        val conn = connection ?: return false
        return try {
            store.flush()
            syncMenu(conn)
            uploadOrders(conn)
            uploadRefunds(conn)
            val now = clock()
            dao.putMeta(MetaEntity(KEY_LAST_SYNC, now.toString()))
            _status.update { it.copy(lastSyncAt = now, error = null) }
            true
        } catch (e: ApiException) {
            failed(e)
            false
        }
    }

    // ---------------------------------------------------------- online orders

    /** Refreshes [onlineOrders] every [intervalMillis] while connected. Runs until cancelled. */
    suspend fun runOnlineEvery(intervalMillis: Long = ONLINE_INTERVAL_MILLIS) {
        while (true) {
            if (connection != null) refreshOnline() else _onlineOrders.value = OnlineOrders()
            delay(intervalMillis)
        }
    }

    suspend fun refreshOnline(): Boolean {
        ready.await()
        val conn = connection ?: return false
        return onlineMutex.withLock {
            try {
                val orders = api.onlineOrders(conn.server, conn.key).orders
                val waiting = orders.filter { it.status == "PENDING" }.map { it.id }.toSet()
                val seen = waitingSeen
                if (seen != null && (waiting - seen).isNotEmpty()) onNewOnlineOrder()
                waitingSeen = waiting
                _onlineOrders.value = OnlineOrders(orders = orders, loaded = true)
                true
            } catch (e: ApiException) {
                if (e is ApiException.Unauthorized) failed(e)
                _onlineOrders.update {
                    it.copy(error = if (e is ApiException.Offline) "No internet, so new online orders can't come in." else e.message)
                }
                false
            }
        }
    }

    /**
     * Moves an online order on (see FireLord's pos/online.py for which moves are allowed).
     * COMPLETED needs [payment] ("CASH" or "CARD") and also saves the sale on this tablet.
     * Returns null when done, or what went wrong.
     */
    suspend fun setOnlineStatus(
        order: OnlineOrderDto,
        status: String,
        payment: String = "",
        by: String = "",
        reason: String = ""
    ): String? {
        ready.await()
        val conn = connection ?: return "This tablet isn't connected to FireLord."
        val problem = try {
            val updated = api.setOnlineStatus(conn.server, conn.key, order.id, StatusUpload(status, payment, by.take(60), reason))
            if (updated.status == "COMPLETED") store.recordOnlineSale(updated.toOrder(clock()).copy(takenBy = by))
            null
        } catch (e: ApiException.Offline) {
            "No internet. The order wasn't changed."
        } catch (e: ApiException) {
            if (e is ApiException.Unauthorized) failed(e)
            e.message ?: "FireLord refused the change."
        }
        refreshOnline()
        return problem
    }

    // ------------------------------------------------------------------- menu

    override val isConnected: Boolean get() = connection != null

    override suspend fun publish(menu: List<MenuItem>, toppings: List<Topping>): MenuChange {
        ready.await()
        return mutex.withLock {
            val conn = connection ?: return@withLock MenuChange.Failed("Not connected to FireLord")
            try {
                if (menuVersion == FIRST_MENU) syncMenu(conn)
                val answer = api.putMenu(
                    conn.server, conn.key,
                    MenuUpload(menuVersion, menu.map { it.toDto() }, toppings.map { it.toDto() })
                )
                pendingAvailability.clear() // the new menu carries the current sold-out switches
                savePendingAvailability()
                applyMenu(answer)
                MenuChange.Saved
            } catch (e: ApiException.Conflict) {
                runCatching { applyMenu(api.menu(conn.server, conn.key, menuVersion)) }
                MenuChange.Conflict
            } catch (e: ApiException.Offline) {
                MenuChange.Offline
            } catch (e: ApiException) {
                failed(e)
                MenuChange.Failed(e.message ?: "FireLord refused the change")
            }
        }
    }

    override fun availabilityChanged(itemId: String, available: Boolean) {
        scope.launch {
            ready.await()
            mutex.withLock {
                if (connection == null) return@withLock
                pendingAvailability[itemId] = available
                savePendingAvailability()
            }
            requestSync()
        }
    }

    private suspend fun syncMenu(conn: Connection) {
        if (menuVersion == FIRST_MENU) {
            firstMenu(conn)
        } else {
            applyMenu(api.menu(conn.server, conn.key, menuVersion))
        }
        if (pendingAvailability.isEmpty()) return
        repeat(MAX_MENU_RETRIES) {
            val state = store.state.value
            try {
                val answer = api.putMenu(
                    conn.server, conn.key,
                    MenuUpload(menuVersion, state.menu.map { it.toDto() }, state.toppings.map { it.toDto() })
                )
                pendingAvailability.clear()
                savePendingAvailability()
                applyMenu(answer)
                return
            } catch (e: ApiException.Conflict) {
                // Changed on the web meanwhile: take that menu (keeping our switches) and try again.
                applyMenu(api.menu(conn.server, conn.key, menuVersion))
            }
        }
    }

    /** First sync after connecting: the server's menu wins, unless it has none yet. */
    private suspend fun firstMenu(conn: Connection) {
        val remote = api.menu(conn.server, conn.key, null)
        if (remote.version == 0L && remote.items.isEmpty() && remote.toppings.isEmpty()) {
            store.flush()
            val state = store.state.value
            val answer = try {
                api.putMenu(
                    conn.server, conn.key,
                    MenuUpload(0, state.menu.map { it.toDto() }, state.toppings.map { it.toDto() })
                )
            } catch (e: ApiException.Conflict) {
                api.menu(conn.server, conn.key, null) // another tablet was faster: use its menu
            }
            applyMenu(answer)
        } else {
            applyMenu(remote)
        }
        pendingAvailability.clear()
        savePendingAvailability()
    }

    private suspend fun applyMenu(answer: MenuResponse) {
        if (answer.changed) {
            val ids = answer.items.map { it.id }.toSet()
            pendingAvailability.keys.retainAll(ids)
            val items = answer.items.map { dto ->
                val item = dto.toModel()
                pendingAvailability[item.id]?.let { item.copy(available = it) } ?: item
            }
            store.applyServerMenu(items, answer.toppings.map { it.toModel() }, answer.taxBasisPoints)
        } else {
            store.applyServerTax(answer.taxBasisPoints)
        }
        menuVersion = answer.version
        dao.putMeta(MetaEntity(KEY_MENU_VERSION, menuVersion.toString()))
        if (answer.shopName != _status.value.shopName) {
            dao.putMeta(MetaEntity(KEY_SHOP, answer.shopName))
            _status.update { it.copy(shopName = answer.shopName) }
        }
    }

    // ----------------------------------------------------------------- orders

    private suspend fun uploadOrders(conn: Connection) {
        repeat(MAX_BATCHES) {
            val batch = dao.ordersToUpload(BATCH_SIZE)
            if (batch.isEmpty()) return
            try {
                sendOrders(conn, batch)
            } catch (e: ApiException.Http) {
                if (e.code != 422) throw e
                // One order the server can't read would block the whole batch: send them one by one.
                for (order in batch) {
                    try {
                        sendOrders(conn, listOf(order))
                    } catch (single: ApiException.Http) {
                        if (single.code != 422) throw single
                        dao.setSyncState(order.id, SyncState.REJECTED)
                    }
                }
            }
        }
    }

    private suspend fun sendOrders(conn: Connection, orders: List<OrderEntity>) {
        val lines = dao.linesFor(orders.map { it.id }).groupBy { it.orderId }
        val sent = orders.map { it to it.toDto(lines[it.id].orEmpty()) }
        val results = api.uploadOrders(conn.server, conn.key, sent.map { it.second })
            .results.associateBy { it.id }
        for ((order, dto) in sent) {
            when (results[dto.id]?.result) {
                "accepted", "duplicate" -> {
                    dao.setSyncState(order.id, SyncState.DONE)
                    if (dto.status == "REFUNDED") dao.setRefundSyncState(order.id, order.refundedAt, SyncState.DONE)
                }
                "rejected" -> dao.setSyncState(order.id, SyncState.REJECTED)
                else -> Unit // not in the answer: try again next time
            }
        }
    }

    private suspend fun uploadRefunds(conn: Connection) {
        for (order in dao.refundsToUpload()) {
            val upload = RefundUpload(isoTime(order.refundedAt ?: order.createdAt), order.refundedBy.orEmpty().take(60))
            val state = try {
                api.refund(conn.server, conn.key, serverOrderId(order.id), upload)
                SyncState.DONE
            } catch (e: ApiException.Http) {
                if (e.code != 404 && e.code != 422) throw e
                SyncState.REJECTED
            }
            dao.setRefundSyncState(order.id, order.refundedAt, state)
        }
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun failed(e: ApiException) {
        when (e) {
            is ApiException.Unauthorized -> {
                connection = null
                dao.deleteMeta(KEY_DEVICE_KEY)
                _status.update {
                    it.copy(
                        connected = false, revoked = true,
                        error = "This tablet was disconnected on FireLord. Connect it again with a new code."
                    )
                }
            }
            is ApiException.Offline -> _status.update {
                it.copy(error = "No internet. Orders are kept on the tablet and sent later.")
            }
            is ApiException.Http -> _status.update {
                it.copy(error = if (e.code >= 500) "FireLord had a problem (${e.code}). Trying again later." else "FireLord said: ${e.message}")
            }
            is ApiException.Conflict -> _status.update { it.copy(error = e.message) }
        }
    }

    private suspend fun load() {
        val server = dao.meta(KEY_SERVER).orEmpty()
        val key = dao.meta(KEY_DEVICE_KEY)
        val url = server.toHttpUrlOrNull()
        if (url != null && !key.isNullOrBlank()) {
            connection = Connection(url, key)
            menuVersion = dao.meta(KEY_MENU_VERSION)?.toLongOrNull() ?: FIRST_MENU
            dao.meta(KEY_PENDING_AVAILABILITY)?.takeIf { it.isNotBlank() }?.let { text ->
                runCatching { json.decodeFromString(availabilitySerializer, text) }.getOrNull()
                    ?.let { pendingAvailability.putAll(it) }
            }
        }
        _status.update {
            it.copy(
                connected = connection != null,
                server = server,
                shopName = dao.meta(KEY_SHOP).orEmpty(),
                lastSyncAt = dao.meta(KEY_LAST_SYNC)?.toLongOrNull()
            )
        }
    }

    private suspend fun savePendingAvailability() {
        dao.putMeta(MetaEntity(KEY_PENDING_AVAILABILITY, json.encodeToString(availabilitySerializer, pendingAvailability.toMap())))
    }

    companion object {
        const val SYNC_INTERVAL_MILLIS = 60_000L
        const val ONLINE_INTERVAL_MILLIS = 15_000L
        const val BATCH_SIZE = 200 // the server takes up to 200 orders per request
        private const val MAX_BATCHES = 100
        private const val MAX_MENU_RETRIES = 3
        private const val FIRST_MENU = -1L

        const val KEY_SERVER = "sync_server"
        const val KEY_DEVICE_KEY = "sync_device_key"
        const val KEY_SHOP = "sync_shop_name"
        const val KEY_MENU_VERSION = "sync_menu_version"
        const val KEY_LAST_SYNC = "sync_last_at"
        const val KEY_PENDING_AVAILABILITY = "sync_pending_availability"
    }
}
