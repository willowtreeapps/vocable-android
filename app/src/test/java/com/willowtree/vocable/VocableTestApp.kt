package com.willowtree.vocable

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.mockito.junit.MockitoJUnitRunner

class VocableTestApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@VocableTestApp)
            modules(emptyList())
        }
    }

    internal fun loadModules(module: Module, block: () -> Unit) {
        loadKoinModules(module)
        block()
        unloadKoinModules(module)
    }
}