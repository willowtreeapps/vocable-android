package com.willowtree.vocable.utility

import androidx.test.platform.app.InstrumentationRegistry
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.vocableKoinModule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module

class VocableKoinTestRule(
    private vararg val additionalTestModules: Module
): TestWatcher() {
    private val allModules = vocableKoinModule + vocableTestModule + additionalTestModules.toList()
    
    override fun starting(description: Description?) {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
                modules(allModules)
            }
        } else {
            loadKoinModules(allModules)
        }
        GlobalContext.get().get<VocableSharedPreferences>().clearAll()
    }

    override fun finished(description: Description?) {
        if (GlobalContext.getOrNull() != null) {
            unloadKoinModules(allModules)
        }
    }
}
