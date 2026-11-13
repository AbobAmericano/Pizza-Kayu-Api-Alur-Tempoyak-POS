package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.OrderStatus
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Sen
import com.slicehouse.pos.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

data class OrdersUiState(
    val orders: List<Order>,
    val settings: Settings,
    /** Every order since midnight, refunded ones included. */
    val todayOrders: List<Order>,
    /** Today's paid (not refunded) orders. */
    val completedToday: Int,
    val salesToday: Sen,
    /** Average paid order today, rounded half up to the sen. */
    val avgTicket: Sen
)

/** Order history and today's sales. */
class OrdersViewModel(
    private val store: PosStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    /** Re-checks the date every minute, so "today" rolls over at midnight. */
    private val now = flow {
        while (true) {
            emit(clock())
            delay(60_000)
        }
    }

    val state: StateFlow<OrdersUiState> = combine(store.state, now) { pos, time ->
        ordersState(pos, startOfDay(time, zone))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ordersState(store.state.value, startOfDay(clock(), zone)))

    /** Refunds an order; [approvedBy] is the owner whose PIN approved it. */
    fun refund(orderId: String, approvedBy: String) = store.refund(orderId, approvedBy)

    fun reorder(order: Order) = store.reorder(order)

    companion object {
        fun ordersState(pos: PosState, startOfToday: Long): OrdersUiState {
            val today = pos.orders.filter { it.createdAt >= startOfToday }
            val paid = today.filter { it.status == OrderStatus.COMPLETED }
            val sales = paid.sumOf { it.total }
            return OrdersUiState(
                orders = pos.orders,
                settings = pos.settings,
                todayOrders = today,
                completedToday = paid.size,
                salesToday = sales,
                avgTicket = if (paid.isEmpty()) 0 else (sales + paid.size / 2) / paid.size
            )
        }

        /** Midnight at the start of the day containing [time], in [zone]. */
        fun startOfDay(time: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(time).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
