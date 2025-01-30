package com.willowtree.vocable.utility

import android.app.Application
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin with minimal dependencies needed for view inflation
        startKoin {
            androidContext(this@TestApplication)
            modules(module {
                single { VocableSharedPreferences() }
            })
        }
    }
}