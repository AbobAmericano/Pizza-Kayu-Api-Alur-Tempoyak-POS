package com.slicehouse.pos.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class PrinterDevice(val name: String, val address: String)

/**
 * Small wrapper around the Bluetooth SPP profile used by ESC/POS thermal printers.
 * Pair the printer in Android's Bluetooth settings first, then pick it in the app.
 */
object BluetoothPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

    private fun adapter(context: Context): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    fun isBluetoothOn(context: Context): Boolean {
        if (!hasPermission(context)) return false
        return runCatching { adapter(context)?.isEnabled == true }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(context: Context): List<PrinterDevice> {
        if (!hasPermission(context)) return emptyList()
        val adapter = adapter(context) ?: return emptyList()
        if (!runCatching { adapter.isEnabled }.getOrDefault(false)) return emptyList()
        return runCatching {
            adapter.bondedDevices
                .map { PrinterDevice(it.name?.takeIf(String::isNotBlank) ?: "Unnamed printer", it.address) }
                .sortedBy { it.name.lowercase() }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    suspend fun print(context: Context, address: String, bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (address.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No printer selected"))
            }
            if (!hasPermission(context)) {
                return@withContext Result.failure(SecurityException("Bluetooth permission not granted"))
            }
            val adapter = adapter(context)
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth is not available"))
            if (!runCatching { adapter.isEnabled }.getOrDefault(false)) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is turned off"))
            }
            val device = runCatching { adapter.getRemoteDevice(address) }.getOrElse {
                return@withContext Result.failure(it)
            }

            var lastError: Throwable? = null
            for (factory in socketFactories(device)) {
                val socket = try {
                    factory()
                } catch (error: Throwable) {
                    lastError = error
                    null
                }
                if (socket == null) continue
                try {
                    socket.connect()
                    socket.outputStream.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    }
                    runCatching { socket.close() }
                    return@withContext Result.success(Unit)
                } catch (error: Throwable) {
                    lastError = error
                    runCatching { socket.close() }
                }
            }
            Result.failure(lastError ?: IllegalStateException("Could not connect to the printer"))
        }

    @SuppressLint("MissingPermission")
    private fun socketFactories(device: BluetoothDevice): List<() -> BluetoothSocket> = listOf(
        { device.createRfcommSocketToServiceRecord(SPP_UUID) },
        { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
        {
            // Some low-cost printers only accept the legacy channel-1 socket.
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            method.invoke(device, 1) as BluetoothSocket
        }
    )
}
