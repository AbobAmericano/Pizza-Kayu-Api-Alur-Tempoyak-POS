package com.slicehouse.pos

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.auth.StaffAuth
import com.slicehouse.pos.data.db.PosDatabase
import com.slicehouse.pos.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    /** The connection to FireLord. Creating it hooks it into the store. */
    val sync: SyncManager by lazy {
        SyncManager(
            dao = database.dao(),
            store = store,
            scope = appScope,
            allowHttp = BuildConfig.DEBUG,
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim().ifBlank { "Tablet" }
        ).also { sync ->
            store.menuMaster = sync
            store.onOrdersChanged = { sync.requestSync() }
            sync.onNewOnlineOrder = ::chime
        }
    }

    /** Two short beeps when a new online order comes in. */
    private val tones: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }.getOrNull()
    }

    private fun chime() {
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600) }
    }
}
