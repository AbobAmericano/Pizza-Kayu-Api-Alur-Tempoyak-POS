package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.sync.OnlineOrderDto
import com.slicehouse.pos.data.sync.OnlineOrders
import com.slicehouse.pos.data.sync.SyncManager
import com.slicehouse.pos.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One button on an online order card: the label, the new status and, for pickup, how they paid. */
data class OnlineAction(
    val label: String,
    val status: String,
    val payment: String = "",
    val reason: String = "",
    /** Asks "are you sure?" first. */
    val confirm: String? = null,
    val primary: Boolean = true
)

/** The buttons for an order in [status], in the order they're shown. Same moves as FireLord's web board. */
fun onlineActions(status: String, label: String): List<OnlineAction> = when (status) {
    "PENDING" -> listOf(
        OnlineAction("Accept", "RECEIVED"),
        OnlineAction(
            "Reject", "CANCELLED", reason = "Sorry, the shop couldn't take this order.",
            confirm = "Reject $label? The customer will see it was cancelled.", primary = false
        )
    )
    "RECEIVED" -> listOf(OnlineAction("🔪 Start preparing", "PREPARING"), cancel(label))
    "PREPARING" -> listOf(OnlineAction("🔥 Into the oven", "BAKING"), cancel(label))
    "BAKING" -> listOf(OnlineAction("🍕 Ready for pickup", "READY"))
    "READY" -> listOf(
        OnlineAction("Paid cash", "COMPLETED", payment = "CASH"),
        OnlineAction("Paid card", "COMPLETED", payment = "CARD"),
        OnlineAction(
            "Not collected", "CANCELLED", reason = "The order wasn't collected.",
            confirm = "Mark $label as not collected?", primary = false
        )
    )
    else -> emptyList()
}

private fun cancel(label: String) = OnlineAction(
    "Cancel", "CANCELLED", reason = "Sorry, the shop had to cancel this order.",
    confirm = "Cancel $label? The customer will see it was cancelled.", primary = false
)

fun onlineStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Waiting for you"
    "RECEIVED" -> "Received"
    "PREPARING" -> "Preparing"
    "BAKING" -> "Baking"
    "READY" -> "Ready for pickup"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

/** The Online screen: orders customers placed on the web, and moving them through the kitchen. */
class OnlineViewModel(
    private val sync: SyncManager,
    private val staffName: () -> String
) : ViewModel() {

    val orders: StateFlow<OnlineOrders> = sync.onlineOrders
    val status: StateFlow<SyncStatus> = sync.status

    private val _busy = MutableStateFlow<String?>(null)

    /** Id of the order being changed right now, so its buttons can be disabled. */
    val busy: StateFlow<String?> = _busy.asStateFlow()

    fun refresh() {
        viewModelScope.launch { sync.refreshOnline() }
    }

    /** [onProblem] gets the reason when the change didn't go through. */
    fun act(order: OnlineOrderDto, action: OnlineAction, onProblem: (String) -> Unit) {
        if (_busy.value != null) return
        _busy.value = order.id
        viewModelScope.launch {
            val problem = try {
                sync.setOnlineStatus(order, action.status, action.payment, staffName(), action.reason)
            } finally {
                _busy.value = null
            }
            problem?.let(onProblem)
        }
    }
}
