package com.zaaam.zcut

import android.app.Application
import com.zaaam.zcut.core.AppContainer

class ZcutApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
