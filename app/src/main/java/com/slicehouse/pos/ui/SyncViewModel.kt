package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.sync.SyncManager
import com.slicehouse.pos.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The FireLord card in Settings: connect with a code, see how syncing is going, disconnect. */
class SyncViewModel(private val sync: SyncManager) : ViewModel() {

    val status: StateFlow<SyncStatus> = sync.status

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** [onResult] gets null when connected, or what went wrong. */
    fun connect(server: String, code: String, onResult: (String?) -> Unit) {
        if (_connecting.value) return
        _connecting.value = true
        viewModelScope.launch {
            val problem = try {
                sync.connect(server, code)
            } finally {
                _connecting.value = false
            }
            onResult(problem)
        }
    }

    fun syncNow() {
        sync.requestSync()
    }

    fun disconnect() {
        viewModelScope.launch { sync.disconnect() }
    }
}
