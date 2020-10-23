package com.willowtree.vocable

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) }
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility(get()) }
        single { PresetsViewModel() }
    }
}