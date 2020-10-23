package com

import org.junit.Rule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module

abstract class BaseUnitTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val allUnitTestsModule = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) }
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility(get()) }
        single { PresetsViewModel() }
    }

    @Before
    fun initializeKoin() {
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(listOf(allUnitTestsModule))
        }
    }

    @After
    fun closeKoin() {
        stopKoin()
    }
}