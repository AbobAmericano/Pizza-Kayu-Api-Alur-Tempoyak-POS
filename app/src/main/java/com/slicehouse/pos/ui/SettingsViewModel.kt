package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import kotlinx.coroutines.flow.StateFlow

data class SettingsUiState(val settings: Settings)

/** Shop details, theme, printer and resetting data. */
class SettingsViewModel(private val store: PosStore) : ViewModel() {

    val state: StateFlow<SettingsUiState> = store.state.mapState(viewModelScope) {
        SettingsUiState(settings = it.settings)
    }

    fun updateSettings(settings: Settings) = store.updateSettings(settings)

    /** Deletes every order and restores the starter menu. Shop settings are kept. */
    fun resetAll() = store.resetAll()
}
