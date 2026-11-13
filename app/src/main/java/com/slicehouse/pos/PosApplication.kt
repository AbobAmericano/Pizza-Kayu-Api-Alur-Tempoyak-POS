package com.slicehouse.pos

import android.app.Application

class PosApplication : Application() {
    /** Lives as long as the app process, so state survives rotation and screen changes. */
    val container: AppContainer by lazy { AppContainer(this) }
}
