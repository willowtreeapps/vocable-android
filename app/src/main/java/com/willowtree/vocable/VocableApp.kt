package com.willowtree.vocable

import android.app.Application
import com.willowtree.vocable.di.vocableKoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class VocableApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@VocableApp)

            modules(vocableKoinModule)
        }
    }
}