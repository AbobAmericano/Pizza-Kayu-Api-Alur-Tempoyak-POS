package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.Topping
import kotlinx.coroutines.flow.StateFlow

data class MenuUiState(
    val menu: List<MenuItem>,
    val toppings: List<Topping>,
    val settings: Settings
)

/** Editing the menu and extra toppings. */
class MenuViewModel(private val store: PosStore) : ViewModel() {

    val state: StateFlow<MenuUiState> = store.state.mapState(viewModelScope) {
        MenuUiState(menu = it.menu, toppings = it.toppings, settings = it.settings)
    }

    fun saveMenuItem(item: MenuItem) = store.saveMenuItem(item)
    fun deleteMenuItem(id: String) = store.deleteMenuItem(id)
    fun toggleAvailable(id: String) = store.toggleAvailable(id)
    fun saveTopping(topping: Topping) = store.saveTopping(topping)
    fun deleteTopping(id: String) = store.deleteTopping(id)
}
