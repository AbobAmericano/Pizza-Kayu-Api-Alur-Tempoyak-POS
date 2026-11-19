package com.slicehouse.pos

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.auth.StaffAuth
import com.slicehouse.pos.data.db.PosDatabase
import com.slicehouse.pos.data.sync.FirebaseSyncManager
import com.slicehouse.pos.data.sync.OnlineOrderDto
import com.slicehouse.pos.data.sync.SyncManager
import com.slicehouse.pos.data.sync.toOrder
import com.slicehouse.pos.print.BluetoothPrinter
import com.slicehouse.pos.print.ReceiptPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Creates the app's long-lived objects once and hands them out.
 * ViewModels get what they need from here (see ui/PosViewModelFactory.kt),
 * so nothing else has to know how the database or store is built.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Lives as long as the app; background work for the store, sign-in and sync runs here. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** One database for everything. */
    private val database: PosDatabase by lazy { PosDatabase.open(appContext) }

    /** The single source of truth for menu, settings, orders and the cart. */
    val store: PosStore by lazy { PosStore(database.dao(), appContext.filesDir, appScope) }

    /** Staff accounts, who is signed in, and owner approval. */
    val auth: StaffAuth by lazy { StaffAuth(database.staffDao(), appScope) }

    /**
     * Cloud Firestore channel for the customer web app. Null until the owner drops
     * a real app/google-services.json into the project and builds again.
     */
    val firebase: FirebaseSyncManager? by lazy {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            null
        } else {
            FirebaseSyncManager(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), appScope)
                .also { it.startListening() }
        }
    }

    /** The connection to FireLord, plus the optional Firestore web channel. */
    val sync: SyncManager by lazy {
        SyncManager(
            dao = database.dao(),
            store = store,
            scope = appScope,
            allowHttp = BuildConfig.DEBUG,
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim().ifBlank { "Tablet" },
            firebase = firebase
        ).also { sync ->
            store.menuMaster = sync
            store.onOrdersChanged = { sync.requestSync() }
            sync.onNewOnlineOrder = ::chime
            sync.onOnlineKitchenTicket = { order -> printOnline(order, kitchen = true, payment = "") }
            sync.onOnlineReceipt = { order, payment -> printOnline(order, kitchen = false, payment = payment) }
        }
    }

    /** Sends a kitchen ticket or receipt for a web order to the configured Bluetooth printer. */
    private fun printOnline(order: OnlineOrderDto, kitchen: Boolean, payment: String) {
        val settings = store.state.value.settings
        if (settings.printerAddress.isBlank() || !BluetoothPrinter.hasPermission(appContext)) return
        val method = com.slicehouse.pos.data.PaymentMethod.entries
            .firstOrNull { it.name == payment.ifBlank { order.payment } }
            ?: com.slicehouse.pos.data.PaymentMethod.CASH
        val sale = order.toOrder(System.currentTimeMillis()).copy(payment = method)
        val bytes = if (kitchen) ReceiptPrinter.buildKitchenBytes(sale, settings) else ReceiptPrinter.buildReceiptBytes(sale, settings)
        appScope.launch { runCatching { BluetoothPrinter.print(appContext, settings.printerAddress, bytes) } }
    }

    /** Two short beeps when a new online order comes in. */
    private val tones: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }.getOrNull()
    }

    private fun chime() {
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600) }
    }
}
