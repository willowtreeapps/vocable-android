package com.willowtree.vocable

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VocableApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@VocableApp)

            modules(listOf(AppKoinModule.getModule()))
        }
    }
}