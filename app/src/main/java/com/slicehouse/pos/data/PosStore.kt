package com.slicehouse.pos.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class PosState(
    val menu: List<MenuItem> = SampleData.menu,
    val toppings: List<Topping> = SampleData.toppings,
    val settings: Settings = Settings(),
    val orders: List<Order> = emptyList(),
    val nextOrderNumber: Int = 1001,
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
    val subtotal: Double get() = round2(cart.sumOf { it.lineTotal })
    val tax: Double get() = round2(subtotal * settings.taxPercent / 100.0)
    val total: Double get() = round2(subtotal + tax)
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
}

class PosStore(context: Context) {

    private companion object {
        /** v3: shop pizza pricing/sizes, Extra cheese only, kitchen receipts. */
        const val SCHEMA_VERSION = 3
    }

    private val file = File(context.filesDir, "slicehouse_pos.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<PosState> = _state.asStateFlow()

    // ---------------------------------------------------------------- register

    fun addLine(line: CartLine) {
        update { state ->
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
        update { state ->
            val cart = state.cart.mapNotNull {
                if (it.lineId != lineId) return@mapNotNull it
                val qty = it.qty + delta
                if (qty <= 0) null else it.copy(qty = qty)
            }
            state.copy(cart = cart)
        }
    }

    fun removeLine(lineId: String) {
        update { state -> state.copy(cart = state.cart.filterNot { it.lineId == lineId }) }
    }

    fun clearCart() {
        update { state ->
            state.copy(
                cart = emptyList(),
                table = "",
                customerName = "",
                customerPhone = "",
                address = "",
                orderNote = "",
                pickupTime = ""
            )
        }
    }

    fun setOrderType(type: OrderType) = update { it.copy(orderType = type) }
    fun setTable(value: String) = update { it.copy(table = value) }
    fun setCustomerName(value: String) = update { it.copy(customerName = value) }
    fun setCustomerPhone(value: String) = update { it.copy(customerPhone = value) }
    fun setAddress(value: String) = update { it.copy(address = value) }
    fun setOrderNote(value: String) = update { it.copy(orderNote = value) }
    fun setPickupTime(value: String) = update { it.copy(pickupTime = value) }

    /** Converts the current cart into a paid order and clears the register. */
    fun completeOrder(payment: PaymentMethod, cashReceived: Double): Order? {
        val current = _state.value
        if (current.cart.isEmpty()) return null
        val total = current.total
        val received = if (payment == PaymentMethod.CASH) cashReceived else total
        val order = Order(
            id = UUID.randomUUID().toString(),
            number = current.nextOrderNumber,
            createdAt = System.currentTimeMillis(),
            lines = current.cart,
            type = current.orderType,
            table = current.table,
            customerName = current.customerName,
            customerPhone = current.customerPhone,
            address = current.address,
            pickupTime = current.pickupTime,
            subtotal = current.subtotal,
            tax = current.tax,
            total = total,
            payment = payment,
            cashReceived = received,
            change = round2((received - total).coerceAtLeast(0.0)),
            note = current.orderNote
        )
        update {
            it.copy(
                orders = listOf(order) + it.orders,
                nextOrderNumber = it.nextOrderNumber + 1,
                cart = emptyList(),
                table = "",
                customerName = "",
                customerPhone = "",
                address = "",
                orderNote = "",
                pickupTime = ""
            )
        }
        return order
    }

    fun refund(orderId: String) {
        update { state ->
            state.copy(orders = state.orders.map {
                if (it.id == orderId) it.copy(status = OrderStatus.REFUNDED) else it
            })
        }
    }

    fun reorder(order: Order) {
        update { state ->
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

    fun saveMenuItem(item: MenuItem) {
        update { state ->
            val exists = state.menu.any { it.id == item.id }
            val menu = if (exists) {
                state.menu.map { if (it.id == item.id) item else it }
            } else {
                state.menu + item
            }
            state.copy(menu = menu)
        }
    }

    fun deleteMenuItem(id: String) {
        update { state -> state.copy(menu = state.menu.filterNot { it.id == id }) }
    }

    fun toggleAvailable(id: String) {
        update { state ->
            state.copy(menu = state.menu.map {
                if (it.id == id) it.copy(available = !it.available) else it
            })
        }
    }

    fun saveTopping(topping: Topping) {
        update { state ->
            val exists = state.toppings.any { it.id == topping.id }
            val list = if (exists) {
                state.toppings.map { if (it.id == topping.id) topping else it }
            } else {
                state.toppings + topping
            }
            state.copy(toppings = list)
        }
    }

    fun deleteTopping(id: String) {
        update { state -> state.copy(toppings = state.toppings.filterNot { it.id == id }) }
    }

    // ---------------------------------------------------------------- settings

    fun updateSettings(settings: Settings) = update { it.copy(settings = settings) }

    fun resetAll() {
        update { state ->
            PosState(settings = state.settings, nextOrderNumber = 1001)
        }
    }

    // --------------------------------------------------------------- internals

    private fun sameLine(a: CartLine, b: CartLine): Boolean =
        a.itemId == b.itemId &&
            a.unitPrice == b.unitPrice &&
            a.sizeLabel == b.sizeLabel &&
            a.crustLabel == b.crustLabel &&
            a.toppings == b.toppings &&
            a.note == b.note

    private fun update(block: (PosState) -> PosState) {
        val old = _state.value
        val new = block(old)
        _state.value = new
        val persistedChanged = old.menu != new.menu ||
            old.toppings != new.toppings ||
            old.settings != new.settings ||
            old.orders != new.orders ||
            old.nextOrderNumber != new.nextOrderNumber
        if (persistedChanged) persist(new)
    }

    private fun persist(state: PosState) {
        runCatching {
            val data = PersistedData(
                menu = state.menu,
                toppings = state.toppings,
                settings = state.settings,
                orders = state.orders,
                nextOrderNumber = state.nextOrderNumber,
                schemaVersion = SCHEMA_VERSION
            )
            file.writeText(json.encodeToString(data))
        }
    }

    private fun load(): PosState = runCatching {
        if (!file.exists()) return PosState()
        val data = json.decodeFromString<PersistedData>(file.readText())
        val needsUpgrade = data.schemaVersion < SCHEMA_VERSION
        val state = PosState(
            menu = if (needsUpgrade) migrateMenuV3(data.menu) else data.menu,
            toppings = if (needsUpgrade) SampleData.toppings else data.toppings,
            settings = migrateSettings(data.settings),
            orders = data.orders,
            nextOrderNumber = data.nextOrderNumber
        )
        if (needsUpgrade) persist(state)
        state
    }.getOrElse { PosState() }

    /**
     * v3 upgrade:
     *  - replaces the old demo pizzas with the shop's photographed menu,
     *  - moves every known pizza to the new 10"/11"/12" pricing (RM20 base),
     *  - applies the per-pizza 12" availability and RM40 smoke beef/duck price.
     * Custom items and user changes to names, photos and availability are kept.
     */
    private fun migrateMenuV3(menu: List<MenuItem>): List<MenuItem> {
        val oldPizzaIds = (1..8).map { "p$it" }.toSet()
        val hasOldDemo = menu.any { it.id in oldPizzaIds && it.imageKey.isBlank() }
        val base = if (hasOldDemo) {
            val kept = menu.filterNot { it.id in oldPizzaIds }
            val pizzas = SampleData.menu.filter { it.category == Category.PIZZA }
            (pizzas + kept).distinctBy { it.id }
        } else {
            menu
        }
        val templates = SampleData.menu
            .filter { it.category == Category.PIZZA }
            .associateBy { it.id }
        return base.map { item ->
            val template = templates[item.id] ?: return@map item
            item.copy(
                price = template.price,
                twelveInchAvailable = template.twelveInchAvailable,
                twelveInchDelta = template.twelveInchDelta
            )
        }
    }

    /** Upgrades the old demo shop details to this shop on first run of the new build. */
    private fun migrateSettings(settings: Settings): Settings {
        if (settings.shopName != "Slice House" || settings.phone != "(555) 012-3456") return settings
        val defaults = Settings()
        return settings.copy(
            shopName = defaults.shopName,
            address = defaults.address,
            phone = defaults.phone,
            footer = defaults.footer,
            currency = defaults.currency
        )
    }
}
