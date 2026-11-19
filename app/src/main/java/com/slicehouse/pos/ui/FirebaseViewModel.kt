package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.sync.FirebaseAdminState
import com.slicehouse.pos.data.sync.FirebaseSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Controls the admin sign-in shown in Settings → Firebase. */
class FirebaseViewModel(private val firebase: FirebaseSyncManager?) : ViewModel() {
    val state: StateFlow<FirebaseAdminState> = firebase?.state ?: MutableStateFlow(FirebaseAdminState())

    val configured: Boolean get() = firebase != null

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        val client = firebase ?: run {
            onResult("Firebase isn't configured. Add app/google-services.json and rebuild.")
            return
        }
        viewModelScope.launch { onResult(client.signInAdmin(email, password)) }
    }

    fun signOut() {
        viewModelScope.launch { firebase?.signOut() }
    }
}
