package com.slicehouse.pos.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.print.BluetoothPrinter
import com.slicehouse.pos.print.ReceiptPrinter
import kotlinx.coroutines.launch

/**
 * Print buttons shared by the receipt dialog, the order detail sheet and the
 * pre-payment kitchen ticket. Handles the Android 12+ Bluetooth permission
 * prompt and reports failures as a toast.
 */
@Composable
fun PrintReceiptButton(
    order: Order,
    settings: Settings,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    PrintActionButton(
        label = "Print",
        settings = settings,
        modifier = modifier,
        filled = filled,
        buildBytes = { ReceiptPrinter.buildReceiptBytes(order, settings) }
    )
}

/** Prints a simple kitchen ticket with no prices, social links or footer. */
@Composable
fun PrintKitchenButton(
    order: Order,
    settings: Settings,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    PrintActionButton(
        label = "Kitchen receipt",
        settings = settings,
        modifier = modifier,
        filled = filled,
        buildBytes = { ReceiptPrinter.buildKitchenBytes(order, settings) }
    )
}

@Composable
private fun PrintActionButton(
    label: String,
    settings: Settings,
    modifier: Modifier,
    filled: Boolean,
    buildBytes: () -> ByteArray
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var printing by remember { mutableStateOf(false) }

    fun printNow() {
        val address = settings.printerAddress
        if (address.isBlank()) {
            Toast.makeText(context, "Choose a Bluetooth printer in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            printing = true
            val result = BluetoothPrinter.print(context, address, buildBytes())
            printing = false
            val message = result.fold(
                onSuccess = { "Sent to ${settings.printerName.ifBlank { "printer" }}" },
                onFailure = { "Print failed: ${it.message ?: "check the printer"}" }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            printNow()
        } else {
            Toast.makeText(context, "Bluetooth permission is needed to print", Toast.LENGTH_LONG).show()
        }
    }

    val onClick: () -> Unit = {
        when {
            settings.printerAddress.isBlank() ->
                Toast.makeText(context, "Choose a Bluetooth printer in Settings first", Toast.LENGTH_LONG).show()
            !BluetoothPrinter.hasPermission(context) ->
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            else -> printNow()
        }
    }

    val text = if (printing) "Printing…" else label
    val icon: @Composable () -> Unit = {
        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    if (filled) {
        Button(onClick = onClick, enabled = !printing, modifier = modifier) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = !printing, modifier = modifier) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(text)
        }
    }
}
