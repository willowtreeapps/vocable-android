package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.splash.SplashViewModel
import com.willowtree.vocable.utils.IdlingResourceContainer
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

inline fun <reified T> Module.idlingResourceContainerOf() = single(named<T>()) {
    IdlingResourceContainerTestingImpl(T::class.java.simpleName)
} bind IdlingResourceContainer::class

val idlingResourceContainerModule = module {
    idlingResourceContainerOf<SplashViewModel>()
    idlingResourceContainerOf<PresetsViewModel>()
}

class IdlingResourceTestRule : TestWatcher(), KoinComponent {
    private val splashActivityContainer: IdlingResourceContainerTestingImpl by inject(named<SplashViewModel>())
    private val mainActivityContainer: IdlingResourceContainerTestingImpl by inject(named<PresetsViewModel>())

    override fun starting(description: Description?) {
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        loadKoinModules(idlingResourceContainerModule)
        IdlingRegistry.getInstance().register(
            splashActivityContainer.idlingResource,
            mainActivityContainer.idlingResource
        )
    }

    override fun finished(description: Description?) {
        IdlingRegistry.getInstance().unregister(
            splashActivityContainer.idlingResource,
            mainActivityContainer.idlingResource
        )
    }
}