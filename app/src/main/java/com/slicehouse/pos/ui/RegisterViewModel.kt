package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import com.slicehouse.pos.data.CartLine
import com.slicehouse.pos.data.Category
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.OrderType
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import kotlinx.coroutines.flow.StateFlow

/** The register: menu browsing and the cart being built. */
class RegisterViewModel(private val store: PosStore) : ViewModel() {

    /** The register works on the whole POS state: menu, settings and the current cart. */
    val state: StateFlow<PosState> = store.state

    fun addLine(line: CartLine) = store.addLine(line)
    fun changeQty(lineId: String, delta: Int) = store.changeQty(lineId, delta)
    fun clearCart() = store.clearCart()
    fun setOrderType(type: OrderType) = store.setOrderType(type)
    fun setTable(value: String) = store.setTable(value)
    fun setCustomerName(value: String) = store.setCustomerName(value)
    fun setCustomerPhone(value: String) = store.setCustomerPhone(value)
    fun setAddress(value: String) = store.setAddress(value)
    fun setPickupTime(value: String) = store.setPickupTime(value)

    companion object {
        /**
         * Items shown in the menu grid. Hidden items never show. A search looks
         * across every category; otherwise only the chosen category is shown.
         */
        fun filterMenu(menu: List<MenuItem>, category: Category, query: String): List<MenuItem> {
            val available = menu.filter { it.available }
            return if (query.isNotBlank()) {
                available.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                }
            } else {
                available.filter { it.category == category }
            }
        }
    }
}
