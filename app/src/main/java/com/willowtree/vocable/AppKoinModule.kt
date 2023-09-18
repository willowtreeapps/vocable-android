package com.willowtree.vocable

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.settings.AddUpdateCategoryViewModel
import com.willowtree.vocable.settings.EditCategoriesViewModel
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import com.willowtree.vocable.utils.JavaDateProvider
import com.willowtree.vocable.utils.JavaLocaleProvider
import com.willowtree.vocable.utils.LocaleProvider
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.RandomUUIDProvider
import com.willowtree.vocable.utils.UUIDProvider
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) } bind IPresetsRepository::class
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility() } bind ILocalizedResourceUtility::class
        single { CategoriesUseCase(get()) }
        single { RandomUUIDProvider() } bind UUIDProvider::class
        single { JavaDateProvider() } bind DateProvider::class
        single { JavaLocaleProvider() } bind LocaleProvider::class
        viewModel { PresetsViewModel(get(), get()) }
        viewModel { EditCategoriesViewModel(get(), get()) }
        viewModel { AddUpdateCategoryViewModel(get(), get(), get(), get(), get()) }
    }
}