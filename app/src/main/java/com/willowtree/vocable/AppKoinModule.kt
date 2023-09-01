package com.willowtree.vocable

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) } bind IPresetsRepository::class
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility() }
        single { CategoriesUseCase(get()) }
        viewModel { PresetsViewModel(get(), get()) }
    }
}