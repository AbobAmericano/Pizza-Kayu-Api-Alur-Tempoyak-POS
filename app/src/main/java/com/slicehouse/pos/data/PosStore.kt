package com.slicehouse.pos.data

import com.slicehouse.pos.data.db.MetaEntity
import com.slicehouse.pos.data.db.PosDao
import com.slicehouse.pos.data.db.lineEntities
import com.slicehouse.pos.data.db.toEntity
import com.slicehouse.pos.data.db.toMenuEntities
import com.slicehouse.pos.data.db.toModel
import com.slicehouse.pos.data.db.toOrders
import com.slicehouse.pos.data.db.toToppingEntities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PosState(
    val menu: List<MenuItem> = SampleData.menu,
    val toppings: List<Topping> = SampleData.toppings,
    val settings: Settings = Settings(),
    val orders: List<Order> = emptyList(),
    val nextOrderNumber: Int = FIRST_ORDER_NUMBER,
    /** False until the database has been read; the UI shows a loading screen. */
    val loaded: Boolean = false,
    /** Shown as a banner when data couldn't be imported or saved. */
    val storageWarning: String? = null,
    // Transient (never persisted) register state.
    val cart: List<CartLine> = emptyList(),
    val orderType: OrderType = OrderType.TAKEAWAY,
    val table: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val orderNote: String = "",
    val pickupTime: String = ""
) {
    val subtotal: Sen get() = cart.sumOf { it.lineTotal }
    val tax: Sen get() = taxSen(subtotal, settings.taxBasisPoints)
    val total: Sen get() = subtotal + tax
    val itemCount: Int get() = cart.sumOf { it.qty }

    /**
     * Snapshot of the current cart for a kitchen ticket printed before payment.
     * Uses the order number the paid receipt will get, without consuming it.
     */
    fun kitchenTicket(): Order? {
        if (cart.isEmpty()) return null
        return Order(
            id = "kitchen-${nextOrderNumber}",
            number = nextOrderNumber,
            createdAt = System.currentTimeMillis(),
            lines = cart,
            type = orderType,
            table = table,
            customerName = customerName,
            customerPhone = customerPhone,
            address = address,
            note = orderNote,
            pickupTime = pickupTime
        )
    }

    /** The paid order the current cart becomes, or null if the cart is empty. */
    fun buildOrder(payment: PaymentMethod, cashReceived: Sen, id: String, now: Long, takenBy: String = ""): Order? {
        if (cart.isEmpty()) return null
        val received = if (payment == PaymentMethod.CASH) cashReceived else total
        return Order(
            id = id,
            number = nextOrderNumber,
            createdAt = now,
            lines = cart,
            type = orderType,
            table = table,
            customerName = customerName,
            customerPhone = customerPhone,
            address = address,
            pickupTime = pickupTime,
            subtotal = subtotal,
            tax = tax,
            total = total,
            payment = payment,
            cashReceived = received,
            change = (received - total).coerceAtLeast(0),
            note = orderNote,
            takenBy = takenBy
        )
    }

    fun clearedRegister(): PosState = copy(
        cart = emptyList(),
        table = "",
        customerName = "",
        customerPhone = "",
        address = "",
        orderNote = "",
        pickupTime = ""
    )

    companion object {
        const val FIRST_ORDER_NUMBER = 1001
    }
}

/**
 * Holds the app state and saves it to the Room database.
 *
 * Screens read [state] and call the functions below. Each change updates [state]
 * straight away, then the matching database write is queued and run in order on
 * a background thread. A failed write shows a warning instead of being ignored.
 */
class PosStore(
    private val dao: PosDao,
    private val filesDir: File,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val _state = MutableStateFlow(PosState())
    val state: StateFlow<PosState> = _state.asStateFlow()

    /** Database writes, run one at a time in the order they were made. */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.IO) {
            runCatching { load() }
                .onSuccess { loaded -> _state.value = loaded }
                .onFailure { error ->
                    _state.update {
                        it.copy(loaded = true, storageWarning = "Couldn't open saved data (${error.describe()}).")
                    }
                }
            for (write in writes) {
                runCatching { write() }.onFailure { error ->
                    _state.update {
                        it.copy(storageWarning = "Couldn't save the last change (${error.describe()}).")
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- register

    fun addLine(line: CartLine) {
        _state.update { state ->
            val existing = state.cart.firstOrNull { sameLine(it, line) }
            val cart = if (existing != null) {
                state.cart.map {
                    if (it.lineId == existing.lineId) it.copy(qty = it.qty + line.qty) else it
                }
            } else {
                state.cart + line.copy(lineId = UUID.randomUUID().toString())
            }
            state.copy(cart = cart)
        }
    }

    fun changeQty(lineId: String, delta: Int) {
        _state.update { state ->
            val cart = state.cart.mapNotNull {
                if (it.lineId != lineId) return@mapNotNull it
                val qty = it.qty + delta
                if (qty <= 0) null else it.copy(qty = qty)
            }
            state.copy(cart = cart)
        }
    }

    fun removeLine(lineId: String) {
        _state.update { state -> state.copy(cart = state.cart.filterNot { it.lineId == lineId }) }
    }

    fun clearCart() = _state.update { it.clearedRegister() }

    fun setOrderType(type: OrderType) = _state.update { it.copy(orderType = type) }
    fun setTable(value: String) = _state.update { it.copy(table = value) }
    fun setCustomerName(value: String) = _state.update { it.copy(customerName = value) }
    fun setCustomerPhone(value: String) = _state.update { it.copy(customerPhone = value) }
    fun setAddress(value: String) = _state.update { it.copy(address = value) }
    fun setOrderNote(value: String) = _state.update { it.copy(orderNote = value) }
    fun setPickupTime(value: String) = _state.update { it.copy(pickupTime = value) }

    /** Converts the current cart into a paid order and clears the register. */
    fun completeOrder(payment: PaymentMethod, cashReceived: Sen, takenBy: String = ""): Order? {
        val order = _state.value.buildOrder(payment, cashReceived, UUID.randomUUID().toString(), clock(), takenBy)
            ?: return null
        val next = order.number + 1
        _state.update {
            it.clearedRegister().copy(orders = listOf(order) + it.orders, nextOrderNumber = next)
        }
        enqueue { dao.saveOrder(order.toEntity(), order.lineEntities(), next) }
        return order
    }

    /** Marks an order refunded. [approvedBy] is the owner who approved it. */
    fun refund(orderId: String, approvedBy: String? = null) {
        val now = clock()
        _state.update { state ->
            state.copy(orders = state.orders.map {
                if (it.id == orderId) it.copy(status = OrderStatus.REFUNDED, refundedAt = now, refundedBy = approvedBy) else it
            })
        }
        enqueue { dao.setOrderStatus(orderId, OrderStatus.REFUNDED, now, approvedBy) }
    }

    fun reorder(order: Order) {
        _state.update { state ->
            val lines = order.lines.map { it.copy(lineId = UUID.randomUUID().toString()) }
            state.copy(
                cart = lines,
                orderType = order.type,
                table = order.table,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                address = order.address
            )
        }
    }

    // -------------------------------------------------------------------- menu

    fun saveMenuItem(item: MenuItem) = updateMenu { menu ->
        if (menu.any { it.id == item.id }) menu.map { if (it.id == item.id) item else it } else menu + item
    }

    fun deleteMenuItem(id: String) = updateMenu { menu -> menu.filterNot { it.id == id } }

    fun toggleAvailable(id: String) = updateMenu { menu ->
        menu.map { if (it.id == id) it.copy(available = !it.available) else it }
    }

    fun saveTopping(topping: Topping) = updateToppings { list ->
        if (list.any { it.id == topping.id }) list.map { if (it.id == topping.id) topping else it } else list + topping
    }

    fun deleteTopping(id: String) = updateToppings { list -> list.filterNot { it.id == id } }

    // ---------------------------------------------------------------- settings

    fun updateSettings(settings: Settings) {
        _state.update { it.copy(settings = settings) }
        enqueue { dao.upsertSettings(settings.toEntity()) }
    }

    /** Deletes every order and restores the starter menu. Shop settings are kept. */
    fun resetAll() {
        _state.update {
            PosState(settings = it.settings, loaded = true, storageWarning = it.storageWarning)
        }
        enqueue {
            dao.resetData(
                SampleData.menu.toMenuEntities(),
                SampleData.toppings.toToppingEntities(),
                PosState.FIRST_ORDER_NUMBER
            )
        }
    }

    fun dismissStorageWarning() {
        _state.update { it.copy(storageWarning = null) }
        enqueue { dao.putMeta(MetaEntity(PosDao.KEY_IMPORT_WARNING, "")) }
    }

    /** Suspends until every write queued so far has finished. Used by tests. */
    internal suspend fun awaitWrites() {
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
    }

    // --------------------------------------------------------------- internals

    private fun updateMenu(block: (List<MenuItem>) -> List<MenuItem>) {
        val menu = _state.updateAndGet { it.copy(menu = block(it.menu)) }.menu
        enqueue { dao.replaceMenu(menu.toMenuEntities()) }
    }

    private fun updateToppings(block: (List<Topping>) -> List<Topping>) {
        val toppings = _state.updateAndGet { it.copy(toppings = block(it.toppings)) }.toppings
        enqueue { dao.replaceToppings(toppings.toToppingEntities()) }
    }

    private fun enqueue(write: suspend () -> Unit) {
        writes.trySend(write)
    }

    private fun sameLine(a: CartLine, b: CartLine): Boolean =
        a.itemId == b.itemId &&
            a.unitPrice == b.unitPrice &&
            a.sizeLabel == b.sizeLabel &&
            a.crustLabel == b.crustLabel &&
            a.toppings == b.toppings &&
            a.note == b.note

    private suspend fun load(): PosState {
        if (dao.meta(PosDao.KEY_INITIALIZED) == null) initialize()
        return PosState(
            menu = dao.menu().map { it.toModel() },
            toppings = dao.toppings().map { it.toModel() },
            settings = dao.settings()?.toModel() ?: Settings(),
            orders = toOrders(dao.orders(), dao.orderLines()),
            nextOrderNumber = dao.meta(PosDao.KEY_NEXT_ORDER_NUMBER)?.toIntOrNull()
                ?: PosState.FIRST_ORDER_NUMBER,
            loaded = true,
            storageWarning = dao.meta(PosDao.KEY_IMPORT_WARNING)?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * First launch of this version. Imports v1.4's JSON file if there is one.
     * The old file is never deleted: after a successful import it is renamed to
     * [LegacyImport.IMPORTED_FILE_NAME]; if it can't be read it is left as it is.
     */
    private suspend fun initialize() {
        val legacyFile = File(filesDir, LegacyImport.FILE_NAME)
        val starter = LegacyImport.Data(
            menu = SampleData.menu,
            toppings = SampleData.toppings,
            settings = Settings(),
            orders = emptyList(),
            nextOrderNumber = PosState.FIRST_ORDER_NUMBER
        )
        val imported = if (legacyFile.exists()) {
            runCatching { LegacyImport.parse(legacyFile.readText()) }.getOrNull()
        } else {
            null
        }
        val couldNotRead = "Your old order history couldn't be read, so the app started with the " +
            "starter menu. The old file was kept unchanged (${LegacyImport.FILE_NAME})."

        if (imported != null) {
            val saved = runCatching { initializeWith(imported, warning = "") }.isSuccess
            if (saved) {
                legacyFile.renameTo(File(filesDir, LegacyImport.IMPORTED_FILE_NAME))
                return
            }
        }
        initializeWith(starter, warning = if (legacyFile.exists()) couldNotRead else "")
    }

    private suspend fun initializeWith(data: LegacyImport.Data, warning: String) {
        dao.initialize(
            menu = data.menu.toMenuEntities(),
            toppings = data.toppings.toToppingEntities(),
            settings = data.settings.toEntity(),
            orders = data.orders.map { it.toEntity() },
            lines = data.orders.flatMap { it.lineEntities() },
            nextOrderNumber = data.nextOrderNumber,
            importWarning = warning
        )
    }

    private fun Throwable.describe(): String = message ?: this::class.java.simpleName
}
