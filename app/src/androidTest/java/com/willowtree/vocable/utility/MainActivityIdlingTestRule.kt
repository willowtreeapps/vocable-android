package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingRegistry
import com.willowtree.vocable.utils.MainActivityIdlingResourceContainer
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.bind
import org.koin.dsl.module

val mainActivityIdlingResourceModule = module {
    single { TestMainActivityIdlingResourceContainer() } bind MainActivityIdlingResourceContainer::class
}

class MainActivityIdlingTestRule: TestWatcher(), KoinComponent {
    private val idlingResourceContainer: TestMainActivityIdlingResourceContainer by inject()

    override fun starting(description: Description?) {
        loadKoinModules(mainActivityIdlingResourceModule)
        IdlingRegistry.getInstance().register(idlingResourceContainer.idlingResource)
    }

    override fun finished(description: Description?) {
        IdlingRegistry.getInstance().unregister(idlingResourceContainer.idlingResource)
    }
}