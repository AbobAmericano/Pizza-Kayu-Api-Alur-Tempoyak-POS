package com.slicehouse.pos.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slicehouse.pos.PosApplication
import com.slicehouse.pos.data.PosState
import com.slicehouse.pos.data.PosStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Builds every screen's ViewModel from the [com.slicehouse.pos.AppContainer].
 * Screens ask for one with `viewModel(factory = PosViewModelFactory)`.
 */
val PosViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { AppViewModel(store()) }
    initializer { RegisterViewModel(store()) }
    initializer { CheckoutViewModel(store()) }
    initializer { OrdersViewModel(store()) }
    initializer { MenuViewModel(store()) }
    initializer { SettingsViewModel(store()) }
}

private fun CreationExtras.store(): PosStore =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PosApplication).container.store

/** Turns the store's state into one screen's state, kept up to date while the ViewModel lives. */
internal fun <T> StateFlow<PosState>.mapState(scope: CoroutineScope, transform: (PosState) -> T): StateFlow<T> =
    map(transform).stateIn(scope, SharingStarted.Eagerly, transform(value))
