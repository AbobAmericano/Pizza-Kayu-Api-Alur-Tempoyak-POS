package com.slicehouse.pos

import android.app.Application
import com.slicehouse.pos.data.PosStore

class PosApplication : Application() {
    val store: PosStore by lazy { PosStore(this) }
}
