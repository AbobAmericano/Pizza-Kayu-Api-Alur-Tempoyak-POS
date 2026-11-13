package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import kotlinx.coroutines.flow.StateFlow

/** What the app shell needs: loading, theme and receipt settings, and storage warnings. */
data class AppUiState(
    val loaded: Boolean,
    val settings: Settings,
    val storageWarning: String?
)

class AppViewModel(private val store: PosStore) : ViewModel() {

    val state: StateFlow<AppUiState> = store.state.mapState(viewModelScope) {
        AppUiState(loaded = it.loaded, settings = it.settings, storageWarning = it.storageWarning)
    }

    fun dismissStorageWarning() = store.dismissStorageWarning()
}
