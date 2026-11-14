package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.MenuChange
import com.slicehouse.pos.data.MenuItem
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.Topping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MenuUiState(
    val menu: List<MenuItem>,
    val toppings: List<Topping>,
    val settings: Settings
)

/**
 * Editing the menu and extra toppings. While connected to FireLord an edit is
 * sent to the server first, so each change reports back how it went.
 */
class MenuViewModel(private val store: PosStore) : ViewModel() {

    val state: StateFlow<MenuUiState> = store.state.mapState(viewModelScope) {
        MenuUiState(menu = it.menu, toppings = it.toppings, settings = it.settings)
    }

    private val _saving = MutableStateFlow(false)

    /** True while a change is on its way to FireLord. */
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun saveMenuItem(item: MenuItem, onResult: (MenuChange) -> Unit) = change(onResult) { store.saveMenuItem(item) }
    fun deleteMenuItem(id: String, onResult: (MenuChange) -> Unit) = change(onResult) { store.deleteMenuItem(id) }
    fun saveTopping(topping: Topping, onResult: (MenuChange) -> Unit) = change(onResult) { store.saveTopping(topping) }
    fun deleteTopping(id: String, onResult: (MenuChange) -> Unit) = change(onResult) { store.deleteTopping(id) }
    fun toggleAvailable(id: String) = store.toggleAvailable(id)

    private fun change(onResult: (MenuChange) -> Unit, block: suspend () -> MenuChange) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val result = try {
                block()
            } finally {
                _saving.value = false
            }
            onResult(result)
        }
    }
}

/** What to tell the owner when a menu change didn't go through, or null if it did. */
fun MenuChange.problem(): String? = when (this) {
    MenuChange.Saved -> null
    MenuChange.Offline -> "No internet. Menu changes need FireLord while this tablet is connected. Nothing was changed."
    MenuChange.Conflict -> "The menu was just changed on FireLord. The tablet now shows that menu: check it and try again."
    is MenuChange.Failed -> "Not saved: $reason"
}
