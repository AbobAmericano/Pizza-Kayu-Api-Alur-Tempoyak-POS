package com.slicehouse.pos

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class PosApplication : Application() {
    /** Lives as long as the app process, so state survives rotation and screen changes. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val sync = container.sync // hooks the sync into the store before any screen opens
        // Sync every minute while the app is on screen; stop when it goes to the background.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.STARTED) {
                sync.runEvery()
            }
        }
    }
}
