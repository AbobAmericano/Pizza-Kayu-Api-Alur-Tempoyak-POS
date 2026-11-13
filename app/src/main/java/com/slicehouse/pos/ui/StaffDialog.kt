package com.slicehouse.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slicehouse.pos.data.auth.PinHasher
import com.slicehouse.pos.data.auth.Staff
import com.slicehouse.pos.data.auth.StaffRole

/** Owner-only: add staff, change PINs, turn accounts on or off, new recovery code. */
@Composable
fun StaffDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var pinFor by remember { mutableStateOf<Staff?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.92f)
        ) {
            Column(Modifier.padding(24.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                Text("Staff", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Each person signs in with their own PIN. Owners can approve refunds, prices and settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.padding(top = 12.dp))
                state.staff.forEach { person ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                person.role.label + if (!person.active) " · Off" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { pinFor = person }) { Text("Change PIN") }
                        Switch(
                            checked = person.active,
                            onCheckedChange = { on -> viewModel.setActive(person.id, on) { message = it } }
                        )
                    }
                    HorizontalDivider()
                }
                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { adding = true }) { Text("Add staff") }
                    TextButton(onClick = { viewModel.regenerateRecoveryCode() }) { Text("New recovery code") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }

    if (adding) AddStaffDialog(viewModel, onDismiss = { adding = false })
    pinFor?.let { person -> ChangePinDialog(person, viewModel, onDismiss = { pinFor = null }) }
}

@Composable
private fun AddStaffDialog(viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StaffRole.entries.reversed().forEach { option ->
                        FilterChip(selected = role == option, onClick = { role = option }, label = { Text(option.label) })
                    }
                }
                PinTextField(pin, { pin = it; error = null }, "PIN (4–6 digits)")
                PinTextField(confirm, { confirm = it; error = null }, "Type the PIN again")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = if (pin != confirm) "The two PINs don't match." else null
                if (error == null) viewModel.addStaff(name, role, pin) { problem ->
                    if (problem == null) onDismiss() else error = problem
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ChangePinDialog(person: Staff, viewModel: AuthViewModel, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New PIN for ${person.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinTextField(pin, { pin = it; error = null }, "New PIN (4–6 digits)")
                PinTextField(confirm, { confirm = it; error = null }, "Type the PIN again")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    !PinHasher.isValidPin(pin) -> "PIN must be 4 to 6 digits."
                    pin != confirm -> "The two PINs don't match."
                    else -> null
                }
                if (error == null) viewModel.changePin(person.id, pin) { problem ->
                    if (problem == null) onDismiss() else error = problem
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
