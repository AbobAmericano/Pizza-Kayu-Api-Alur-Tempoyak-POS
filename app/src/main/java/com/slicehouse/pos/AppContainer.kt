package com.slicehouse.pos

import android.content.Context
import com.slicehouse.pos.data.PosStore
import com.slicehouse.pos.data.auth.StaffAuth
import com.slicehouse.pos.data.db.PosDatabase
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

    /** Lives as long as the app; background work for the store and sign-in runs here. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** One database for everything. */
    private val database: PosDatabase by lazy { PosDatabase.open(appContext) }

    /** The single source of truth for menu, settings, orders and the cart. */
    val store: PosStore by lazy { PosStore(database.dao(), appContext.filesDir, appScope) }

    /** Staff accounts, who is signed in, and owner approval. */
    val auth: StaffAuth by lazy { StaffAuth(database.staffDao(), appScope) }
}
