package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.PaymentMethod
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Sen
import com.slicehouse.pos.data.parseMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CheckoutUiState(
    /** The cart, totals and settings being paid for. */
    val pos: PosState,
    val method: PaymentMethod,
    /** Exactly what the cashier typed in the cash field. */
    val cashText: String,
    val due: Sen,
    val change: Sen,
    val shortBy: Sen,
    val canPay: Boolean,
    /** "Exact" plus the next round RM5/10/20/50/100 notes above the amount due. */
    val quickAmounts: List<Sen>,
    /** e.g. "Order #1006 · Takeaway · Table 4 · Pickup 19:30". */
    val orderLabel: String
) {
    val cash: Sen get() = parseMoney(cashText) ?: 0
}

/** Taking payment for the current cart. */
class CheckoutViewModel(private val store: PosStore) : ViewModel() {

    private val method = MutableStateFlow(PaymentMethod.CARD)
    private val cashText = MutableStateFlow("")

    val state: StateFlow<CheckoutUiState> = combine(store.state, method, cashText, ::checkoutState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, checkoutState(store.state.value, method.value, cashText.value))

    fun selectMethod(value: PaymentMethod) {
        method.value = value
    }

    fun setCashText(value: String) {
        cashText.value = value
    }

    /** Records the payment. Returns the paid order, or null if it can't be paid yet. */
    fun pay(): Order? {
        val current = state.value
        if (!current.canPay) return null
        return store.completeOrder(current.method, current.cash)?.also { reset() }
    }

    /** Clears the payment form, e.g. when leaving checkout. */
    fun reset() {
        method.value = PaymentMethod.CARD
        cashText.value = ""
    }

    companion object {
        fun checkoutState(pos: PosState, method: PaymentMethod, cashText: String): CheckoutUiState {
            val due = pos.total
            val cash = parseMoney(cashText) ?: 0
            return CheckoutUiState(
                pos = pos,
                method = method,
                cashText = cashText,
                due = due,
                change = (cash - due).coerceAtLeast(0),
                shortBy = (due - cash).coerceAtLeast(0),
                canPay = pos.cart.isNotEmpty() && (method == PaymentMethod.CARD || cash >= due),
                quickAmounts = quickCashAmounts(due),
                orderLabel = buildString {
                    append("Order #${pos.nextOrderNumber} · ${pos.orderType.label}")
                    if (pos.table.isNotBlank()) append(" · Table ${pos.table}")
                    if (pos.pickupTime.isNotBlank()) append(" · Pickup ${pos.pickupTime}")
                }
            )
        }

        fun quickCashAmounts(due: Sen): List<Sen> {
            val values = mutableListOf(due)
            listOf(500L, 1_000L, 2_000L, 5_000L, 10_000L).forEach { note ->
                val rounded = (due + note - 1) / note * note
                if (rounded > due) values += rounded
            }
            return values.distinct().take(5)
        }
    }
}
