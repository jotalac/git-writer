package dev.jotalac

import android.app.Application
import dev.jotalac.core.di.initFileKitAndroid
import dev.jotalac.core.di.initKoinAndroid

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoinAndroid(this.applicationContext)
    }
}