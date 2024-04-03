package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.splash.SplashViewModel
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.bind
import org.koin.dsl.module

class SplashViewModelTestDouble(
    newPresetsRepository: RoomPresetPhrasesRepository,
    sharedPrefs: VocableSharedPreferences,
    private val idlingResourceContainer: SplashIdlingResourceContainer
): SplashViewModel(newPresetsRepository, sharedPrefs) {

    init {
        idlingResourceContainer.idlingResource.increment()
    }

    override fun postExitSplash() {
        super.postExitSplash()
        idlingResourceContainer.idlingResource.decrement()
    }
}

class SplashIdlingResourceContainer {
    val idlingResource = CountingIdlingResource("SplashViewModelTestDouble")
}

val splashViewModelTestDoubleModule = module {
    single { SplashIdlingResourceContainer() }
    scope<SplashActivity> {
        viewModel { SplashViewModelTestDouble(get(), get(), get()) } bind SplashViewModel::class
    }
}

class SplashIdlingResourceTestRule: TestWatcher(), KoinComponent {
    private val idlingResource: SplashIdlingResourceContainer by inject()

    override fun starting(description: Description?) {
        loadKoinModules(splashViewModelTestDoubleModule)
    }

    fun register() {
        IdlingRegistry.getInstance().register(idlingResource.idlingResource)
    }

    fun unregister() {
        IdlingRegistry.getInstance().unregister(idlingResource.idlingResource)
    }
}