package com.willowtree.vocable

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.room.VocableDatabaseMigrations
import com.willowtree.vocable.utils.LocalizedResourceUtility
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        single { VocableSharedPreferences() }
        single { PresetsRepository(get()) }
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility(get()) }
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

        viewModel { PresetsViewModel(get()) }
    }
}