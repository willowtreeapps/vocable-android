package com.willowtree.vocable

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.presets.RoomPresetCategoriesRepository
import com.willowtree.vocable.room.RoomStoredCategoriesRepository
import com.willowtree.vocable.room.StoredCategoriesRepository
import com.willowtree.vocable.settings.AddUpdateCategoryViewModel
import com.willowtree.vocable.settings.EditCategoriesViewModel
import com.willowtree.vocable.settings.EditCategoryMenuViewModel
import com.willowtree.vocable.settings.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.utils.*
import com.willowtree.vocable.utils.locale.JavaLocaleProvider
import com.willowtree.vocable.utils.locale.LocaleProvider
import com.willowtree.vocable.utils.locale.LocalizedResourceUtility
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {
        // Activity Scope
        scope<MainActivity> {
            scoped {
                FaceTrackingManager(get(), get())
            }
        }

        single { FaceTrackingPermissions(get()) } bind IFaceTrackingPermissions::class
        single { VocableSharedPreferences() } bind IVocableSharedPreferences::class
        single { PresetsRepository(get()) } bind IPresetsRepository::class
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility() } bind ILocalizedResourceUtility::class
        single { CategoriesUseCase(get(), get(), get(), get(), get(), get()) } bind ICategoriesUseCase::class
        single { PhrasesUseCase(get(), get()) }
        single { RandomUUIDProvider() } bind UUIDProvider::class
        single { JavaDateProvider() } bind DateProvider::class
        single { JavaLocaleProvider() } bind LocaleProvider::class
        single { RoomStoredCategoriesRepository(get()) } bind StoredCategoriesRepository::class
        single { RoomPresetCategoriesRepository(get()) } bind PresetCategoriesRepository::class
        viewModel { PresetsViewModel(get(), get()) }
        viewModel { EditCategoriesViewModel(get(), get(), get()) }
        viewModel { AddUpdateCategoryViewModel(get(), get(), get()) }
        viewModel { EditCategoryMenuViewModel(get(), get()) }
        viewModel { SelectionModeViewModel(get()) }
        viewModel { FaceTrackingViewModel() }
    }
}