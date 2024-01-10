package com.willowtree.vocable

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
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
import com.willowtree.vocable.utils.permissions.ActivityPermissionRegisterForLaunch
import com.willowtree.vocable.utils.permissions.ActivityPermissionsDialogShower
import com.willowtree.vocable.utils.permissions.PermissionRequester
import com.willowtree.vocable.utils.permissions.PermissionsDialogShower
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

object AppKoinModule {

    fun getModule() = module {

        scope<MainActivity> {
            scoped {
                FaceTrackingManager(get(), get())
            }
            scoped<PermissionsDialogShower> {
                ActivityPermissionsDialogShower(get())
            }
            scoped<PermissionRequester> {
                ActivityPermissionRegisterForLaunch(get())
            }
            scoped<IFaceTrackingPermissions> {
                FaceTrackingPermissions(get(), get(), get(), get())
            }
            viewModel { FaceTrackingViewModel(get()) }
            viewModel { SelectionModeViewModel(get()) }
        }

        single { VocableSharedPreferences() } bind IVocableSharedPreferences::class
        single { LegacyCategoriesAndPhrasesRepository(get()) } bind ILegacyCategoriesAndPhrasesRepository::class
        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { LocalizedResourceUtility() } bind ILocalizedResourceUtility::class
        single { CategoriesUseCase(get(), get(), get(), get(), get()) } bind ICategoriesUseCase::class
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
    }
}