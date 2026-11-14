package com.slicehouse.pos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slicehouse.pos.data.formatDateTime
import com.slicehouse.pos.data.sync.SyncStatus

/**
 * The FireLord part of Settings. Connecting and disconnecting need the owner
 * ([requireOwner]); "Sync now" doesn't.
 */
@Composable
fun FireLordSection(
    status: SyncStatus,
    connecting: Boolean,
    requireOwner: (() -> Unit) -> Unit,
    onConnect: (server: String, code: String, onResult: (String?) -> Unit) -> Unit,
    onSyncNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    var showConnect by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    if (!status.connected) {
        Text(
            "Connect to FireLord to send every order there and get the menu from there. " +
                "Without it, everything stays on this tablet only.",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        status.error?.takeIf { status.revoked }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { requireOwner { showConnect = true } }, shape = RoundedCornerShape(14.dp)) {
            Text("Connect to FireLord")
        }
    } else {
        Text("Connected to ${status.shopName.ifBlank { "FireLord" }}", fontWeight = FontWeight.SemiBold)
        Text(status.server, style = MaterialTheme.typography.bodySmall, color = muted)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                status.syncing -> "Syncing…"
                status.lastSyncAt != null -> "Last synced ${formatDateTime(status.lastSyncAt)}"
                else -> "Not synced yet"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            if (status.waiting == 0) "Everything is uploaded." else "${status.waiting} waiting to upload",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        if (status.rejected > 0) {
            Text(
                "${status.rejected} older ${if (status.rejected == 1) "order" else "orders"} couldn't be uploaded " +
                    "(their amounts don't add up). They stay on this tablet.",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }
        status.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onSyncNow, enabled = !status.syncing, shape = RoundedCornerShape(14.dp)) {
                Text("Sync now")
            }
            OutlinedButton(
                onClick = { requireOwner { confirmDisconnect = true } },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Disconnect") }
        }
    }

    if (showConnect) {
        ConnectDialog(
            initialServer = status.server,
            connecting = connecting,
            onConnect = onConnect,
            onDismiss = { showConnect = false }
        )
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect from FireLord?") },
            text = {
                Text(
                    "This tablet stops syncing. Orders and the menu stay on it. " +
                        if (status.waiting > 0) "${status.waiting} orders haven't been uploaded yet." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDisconnect()
                    confirmDisconnect = false
                }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ConnectDialog(
    initialServer: String,
    connecting: Boolean,
    onConnect: (server: String, code: String, onResult: (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var server by remember { mutableStateOf(initialServer.removeSuffix("/")) }
    var code by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!connecting) onDismiss() },
        title = { Text("Connect to FireLord") },
        text = {
            Column {
                Text(
                    "On FireLord, open Pizza POS → Tablets → Get a pairing code, then type it here.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it.trim() },
                    label = { Text("FireLord address") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { text -> code = text.filter { it.isDigit() }.take(6) },
                    label = { Text("Pairing code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "If FireLord already has a menu, it replaces this tablet's menu. If not, this tablet's " +
                        "menu is sent up. All orders on this tablet are uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                problem?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    problem = null
                    onConnect(server, code) { result ->
                        if (result == null) onDismiss() else problem = result
                    }
                },
                enabled = !connecting && server.isNotBlank() && code.length == 6
            ) { Text(if (connecting) "Connecting…" else "Connect") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !connecting) { Text("Cancel") }
        }
    )
}
