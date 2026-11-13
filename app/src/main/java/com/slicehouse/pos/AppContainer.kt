package com.slicehouse.pos

import android.content.Context
import com.slicehouse.pos.data.PosStore

/**
 * Creates the app's long-lived objects once and hands them out.
 * ViewModels get what they need from here (see ui/PosViewModelFactory.kt),
 * so nothing else has to know how the database or store is built.
 */
class AppContainer(context: Context) {
    /** The single source of truth for menu, settings, orders and the cart. */
    val store: PosStore by lazy { PosStore(context.applicationContext) }
}
