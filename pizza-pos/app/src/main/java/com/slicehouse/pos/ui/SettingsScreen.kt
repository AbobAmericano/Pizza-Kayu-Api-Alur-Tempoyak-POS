package com.slicehouse.pos.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.ThemeMode
import com.slicehouse.pos.print.BluetoothPrinter
import com.slicehouse.pos.print.ReceiptPrinter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: PosState, store: PosStore) {
    val settings = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf(settings.shopName) }
    var address by remember { mutableStateOf(settings.address) }
    var phone by remember { mutableStateOf(settings.phone) }
    var instagram by remember { mutableStateOf(settings.instagram) }
    var tiktok by remember { mutableStateOf(settings.tiktok) }
    var locationUrl by remember { mutableStateOf(settings.locationUrl) }
    var taxText by remember { mutableStateOf(trimNumber(settings.taxPercent)) }
    var currency by remember { mutableStateOf(settings.currency) }
    var footer by remember { mutableStateOf(settings.footer) }
    var confirmReset by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var testingPrint by remember { mutableStateOf(false) }
    var pendingBluetoothAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val tax = taxText.toDoubleOrNull()
    val valid = shopName.isNotBlank() && currency.isNotBlank() && tax != null && tax >= 0
    val dirty = shopName != settings.shopName ||
        address != settings.address ||
        phone != settings.phone ||
        instagram != settings.instagram ||
        tiktok != settings.tiktok ||
        locationUrl != settings.locationUrl ||
        taxText != trimNumber(settings.taxPercent) ||
        currency != settings.currency ||
        footer != settings.footer

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingBluetoothAction?.invoke()
        } else {
            Toast.makeText(context, "Bluetooth permission is needed to use a printer", Toast.LENGTH_LONG).show()
        }
        pendingBluetoothAction = null
    }

    fun withBluetooth(action: () -> Unit) {
        if (BluetoothPrinter.hasPermission(context)) {
            action()
        } else {
            pendingBluetoothAction = action
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun save() {
        if (!valid) return
        store.updateSettings(
            settings.copy(
                shopName = shopName.trim(),
                address = address.trim(),
                phone = phone.trim(),
                instagram = instagram.trim(),
                tiktok = tiktok.trim(),
                locationUrl = locationUrl.trim(),
                taxPercent = tax ?: settings.taxPercent,
                currency = currency.trim().ifBlank { "RM" },
                footer = footer.trim()
            )
        )
        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    fun openBluetoothSettings() {
        runCatching {
            context.startActivity(Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS))
        }.onFailure {
            Toast.makeText(context, "Could not open Bluetooth settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun runTestPrint() {
        val address = settings.printerAddress
        if (address.isBlank()) {
            Toast.makeText(context, "Choose a printer first", Toast.LENGTH_SHORT).show()
            return
        }
        withBluetooth {
            scope.launch {
                testingPrint = true
                val result = BluetoothPrinter.print(context, address, ReceiptPrinter.buildTestBytes(settings))
                testingPrint = false
                val message = result.fold(
                    onSuccess = { "Test receipt sent" },
                    onFailure = { "Print failed: ${it.message ?: "check the printer"}" }
                )
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text("Store settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { save() },
                enabled = dirty && valid,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            SettingsCard(title = "Shop details", emoji = "🏪") {
                SettingsField("Shop name", shopName) { shopName = it }
                Spacer(Modifier.height(10.dp))
                SettingsField("Address", address) { address = it }
                Spacer(Modifier.height(10.dp))
                SettingsField("Phone", phone) { phone = it }
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Social & location", emoji = "📍") {
                SettingsField("Instagram link", instagram) { instagram = it }
                Spacer(Modifier.height(10.dp))
                SettingsField("TikTok handle", tiktok) { tiktok = it }
                Spacer(Modifier.height(10.dp))
                SettingsField("Google Maps link", locationUrl) { locationUrl = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "These appear on printed and shared receipts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Appearance", emoji = "🎨") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = {
                                store.updateSettings(settings.copy(themeMode = mode))
                            },
                            label = { Text("${mode.emoji} ${mode.label}") },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "System follows the phone's light or dark setting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Bluetooth printer", emoji = "🖨️") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            settings.printerName.ifBlank { "No printer selected" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (settings.printerAddress.isBlank()) "Pair an ESC/POS thermal printer"
                            else settings.printerAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (settings.printerAddress.isNotBlank()) {
                        TextButton(
                            onClick = {
                                store.updateSettings(
                                    settings.copy(printerAddress = "", printerName = "")
                                )
                            }
                        ) { Text("Remove") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { withBluetooth { showPrinterDialog = true } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Choose printer", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { runTestPrint() },
                        enabled = settings.printerAddress.isNotBlank() && !testingPrint,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (testingPrint) "Printing…" else "Test print")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { openBluetoothSettings() }) {
                    Text("Open Android Bluetooth settings")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Paper width",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(58, 80).forEach { width ->
                        FilterChip(
                            selected = settings.paperWidthMm == width,
                            onClick = {
                                store.updateSettings(settings.copy(paperWidthMm = width))
                            },
                            label = { Text("$width mm") },
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Works over Bluetooth SPP with ESC/POS 58 mm and 80 mm printers. " +
                        "Pair the printer in Android Bluetooth settings first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Tax & currency", emoji = "🧾") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = { text ->
                            taxText = text.replace(',', '.').filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Tax rate") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { if (it.length <= 4) currency = it },
                        label = { Text("Currency") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.width(120.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tax is calculated on the order subtotal. Currency is shown in front of every amount.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Receipt", emoji = "📝") {
                OutlinedTextField(
                    value = footer,
                    onValueChange = { footer = it },
                    label = { Text("Footer message") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Printed and shared receipts include this message.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            SettingsCard(title = "Data", emoji = "💾") {
                Text(
                    "Orders, the menu and these settings are stored only on this device. Back up the device to keep your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { confirmReset = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset all data")
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Pizza Kayu Api Dungun POS · v1.4\nMade for pizza people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPrinterDialog) {
        var devices by remember { mutableStateOf(BluetoothPrinter.bondedDevices(context)) }
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Choose a printer") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Pair your printer in Android Bluetooth settings first, then pick it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    if (!BluetoothPrinter.isBluetoothOn(context)) {
                        Text(
                            "Bluetooth is off or permission was not granted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (devices.isEmpty()) {
                        Text("No paired printers found.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        devices.forEach { device ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        store.updateSettings(
                                            settings.copy(
                                                printerAddress = device.address,
                                                printerName = device.name
                                            )
                                        )
                                        showPrinterDialog = false
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.printerAddress == device.address,
                                    onClick = {
                                        store.updateSettings(
                                            settings.copy(
                                                printerAddress = device.address,
                                                printerName = device.name
                                            )
                                        )
                                        showPrinterDialog = false
                                    }
                                )
                                Column(Modifier.padding(start = 4.dp)) {
                                    Text(device.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { devices = BluetoothPrinter.bondedDevices(context) }) {
                        Text("Refresh list")
                    }
                    TextButton(onClick = { openBluetoothSettings() }) {
                        Text("Open Bluetooth settings")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterDialog = false }) { Text("Close") }
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all data?") },
            text = {
                Text(
                    "This deletes every saved order and restores the sample menu and toppings. " +
                        "Your shop details, tax rate and currency are kept. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.resetAll()
                        confirmReset = false
                        Toast.makeText(context, "All data reset", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
