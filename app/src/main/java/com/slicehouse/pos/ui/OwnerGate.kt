package com.slicehouse.pos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.slicehouse.pos.data.auth.PinResult
import com.slicehouse.pos.data.auth.Staff

/**
 * Wraps owner-only actions. If the owner is signed in the action runs straight
 * away; otherwise an "Owner approval" PIN pad appears first.
 *
 * ```
 * val gate = rememberOwnerGate()
 * Button(onClick = { gate.require { owner -> viewModel.refund(id, owner.name) } })
 * ```
 */
class OwnerGate internal constructor(private val request: ((Staff) -> Unit) -> Unit) {
    /** Runs [action] once an owner has approved; it receives the approving owner. */
    fun require(action: (Staff) -> Unit) = request(action)
}

@Composable
fun rememberOwnerGate(viewModel: AuthViewModel = viewModel(factory = PosViewModelFactory)): OwnerGate {
    var pending by remember { mutableStateOf<((Staff) -> Unit)?>(null) }
    val gate = remember(viewModel) {
        OwnerGate { action ->
            val me = viewModel.state.value.signedIn
            if (me?.isOwner == true) action(me) else pending = action
        }
    }
    pending?.let { action ->
        OwnerPinDialog(
            viewModel = viewModel,
            onDismiss = { pending = null },
            onApproved = { owner ->
                pending = null
                action(owner)
            }
        )
    }
    return gate
}

@Composable
private fun OwnerPinDialog(viewModel: AuthViewModel, onDismiss: () -> Unit, onApproved: (Staff) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Owner approval", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Ask the owner to enter their PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                PinPad(
                    pin = pin,
                    onPinChange = {
                        pin = it
                        message = null
                    },
                    onSubmit = {
                        val attempt = pin
                        pin = ""
                        busy = true
                        viewModel.approveAsOwner(attempt) { result ->
                            busy = false
                            if (result is PinResult.Success) onApproved(result.staff) else message = pinMessage(result)
                        }
                    },
                    enabled = !busy
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}
