package com.willowtree.vocable.utility

import androidx.test.espresso.IdlingRegistry
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.IPhrasesUseCase
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetsViewModel
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.dsl.bind
import org.koin.dsl.module

class PresetsViewModelTestDouble(
    categoriesUseCase: ICategoriesUseCase,
    phrasesUseCase: IPhrasesUseCase,
    private val idlingResource: MainActivityIdlingResource
): PresetsViewModel(categoriesUseCase, phrasesUseCase) {
    override suspend fun mapCategoryIdToPhrases(categoryId: String?): List<Phrase?> {
        return super.mapCategoryIdToPhrases(categoryId).apply {
            idlingResource.setIdle()
        }
    }
}

val presetsViewModelTestDoubleModule = module {
    single { MainActivityIdlingResource() }
    viewModel { PresetsViewModelTestDouble(get(), get(), get()) } bind PresetsViewModel::class
}

class MainActivityIdlingTestRule: TestWatcher(), KoinComponent {
    private val idlingResource: MainActivityIdlingResource by inject()

    override fun starting(description: Description?) {
        loadKoinModules(presetsViewModelTestDoubleModule)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    override fun finished(description: Description?) {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}