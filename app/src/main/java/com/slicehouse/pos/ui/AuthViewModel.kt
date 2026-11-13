package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slicehouse.pos.data.auth.AuthState
import com.slicehouse.pos.data.auth.PinResult
import com.slicehouse.pos.data.auth.StaffAuth
import com.slicehouse.pos.data.auth.StaffRole
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Setup, lock screen, owner approval and staff management.
 * PIN hashing is slow on purpose, so every check runs in the background and
 * reports back through a callback on the main thread.
 */
class AuthViewModel(private val auth: StaffAuth) : ViewModel() {

    val state: StateFlow<AuthState> = auth.state

    /** Set right after setup or recovery; shown once, then cleared. */
    val newRecoveryCode: StateFlow<String?> = auth.newRecoveryCode

    fun setUpOwner(name: String, pin: String, onError: (String) -> Unit) {
        viewModelScope.launch { auth.setUpOwner(name, pin)?.let(onError) }
    }

    fun signIn(staffId: String, pin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(auth.signIn(staffId, pin)) }
    }

    fun approveAsOwner(pin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(auth.approveAsOwner(pin)) }
    }

    fun recoverOwner(code: String, newPin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(auth.recoverOwner(code, newPin)) }
    }

    fun lock() = auth.lock()

    fun recoveryCodeShown() = auth.recoveryCodeShown()

    fun addStaff(name: String, role: StaffRole, pin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(auth.addStaff(name, role, pin)) }
    }

    fun changePin(staffId: String, pin: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(auth.changePin(staffId, pin)) }
    }

    fun setActive(staffId: String, active: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(auth.setActive(staffId, active)) }
    }

    fun regenerateRecoveryCode() {
        viewModelScope.launch { auth.regenerateRecoveryCode() }
    }
}
