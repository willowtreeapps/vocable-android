package com.willowtree.vocable.utility

import androidx.test.platform.app.InstrumentationRegistry
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.vocableKoinModule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

class VocableKoinTestRule(
    private vararg val testModules: Module
): TestWatcher() {
    override fun starting(description: Description?) {
        startKoin {
            androidContext(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
            modules(
                vocableKoinModule +
                inMemoryDatabaseModule +
                testModules.toList()
            )
        }.koin.apply {
            get<VocableSharedPreferences>().clearAll()
        }
    }

    override fun finished(description: Description?) {
        stopKoin()
    }
}
