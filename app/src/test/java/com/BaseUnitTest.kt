package com

import android.content.Context
import org.junit.Rule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.room.VocableDatabaseMigrations
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mockito.Mockito.mock

abstract class BaseUnitTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val allUnitTestsModule = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) }
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility(get()) }
        single { PresetsViewModel(get()) }
        single {
            var vocableDatabase: VocableDatabase? = null

            if (vocableDatabase == null) {
                vocableDatabase =
                    Room.databaseBuilder(androidContext(), VocableDatabase::class.java,
                        "VocableDatabase"
                    )
                        .addMigrations(
                            VocableDatabaseMigrations.MIGRATION_1_2,
                            VocableDatabaseMigrations.MIGRATION_2_3,
                            VocableDatabaseMigrations.MIGRATION_3_4,
                            VocableDatabaseMigrations.MIGRATION_4_5
                        )
                        .build()
            }
            vocableDatabase as VocableDatabase
        }
    }

    @Before
    fun initializeKoin() {
        Dispatchers.setMain(testDispatcher)

        startKoin {
            androidContext(mock(Context::class.java))
            modules(listOf(allUnitTestsModule))
        }
    }

    @After
    fun closeKoin() {
        stopKoin()
    }
}