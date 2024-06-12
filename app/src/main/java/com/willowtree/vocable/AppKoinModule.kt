package com.willowtree.vocable

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.presets.RoomPresetCategoriesRepository
import com.willowtree.vocable.room.PresetPhrasesRepository
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.RoomStoredCategoriesRepository
import com.willowtree.vocable.room.RoomStoredPhrasesRepository
import com.willowtree.vocable.room.StoredCategoriesRepository
import com.willowtree.vocable.room.StoredPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.settings.AddUpdateCategoryViewModel
import com.willowtree.vocable.settings.EditCategoriesViewModel
import com.willowtree.vocable.settings.EditCategoryMenuViewModel
import com.willowtree.vocable.settings.EditCategoryPhrasesViewModel
import com.willowtree.vocable.settings.customcategories.CustomCategoryPhraseViewModel
import com.willowtree.vocable.settings.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.splash.SplashActivity
import com.willowtree.vocable.splash.SplashViewModel
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.FaceTrackingManager
import com.willowtree.vocable.utils.FaceTrackingPermissions
import com.willowtree.vocable.utils.IFaceTrackingPermissions
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import com.willowtree.vocable.utils.IVocableSharedPreferences
import com.willowtree.vocable.utils.IdlingResourceContainer
import com.willowtree.vocable.utils.IdlingResourceContainerImpl
import com.willowtree.vocable.utils.JavaDateProvider
import com.willowtree.vocable.utils.RandomUUIDProvider
import com.willowtree.vocable.utils.UUIDProvider
import com.willowtree.vocable.utils.VocableEnvironment
import com.willowtree.vocable.utils.VocableEnvironmentImpl
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.utils.locale.JavaLocaleProvider
import com.willowtree.vocable.utils.locale.LocaleProvider
import com.willowtree.vocable.utils.locale.LocalizedResourceUtility
import com.willowtree.vocable.utils.permissions.ActivityPermissionRegisterForLaunch
import com.willowtree.vocable.utils.permissions.ActivityPermissionsChecker
import com.willowtree.vocable.utils.permissions.ActivityPermissionsRationaleDialogShower
import com.willowtree.vocable.utils.permissions.PermissionRequester
import com.willowtree.vocable.utils.permissions.PermissionsChecker
import com.willowtree.vocable.utils.permissions.PermissionsRationaleDialogShower
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module


val vocableKoinModule = module {

    scope<SplashActivity> {
        viewModel { SplashViewModel(get(), get(), get(named<SplashViewModel>())) }
    }

    scope<MainActivity> {
        scoped {
            FaceTrackingManager(get(), get())
        }
        scoped<PermissionsRationaleDialogShower> {
            ActivityPermissionsRationaleDialogShower(get())
        }
        scoped<PermissionRequester> {
            ActivityPermissionRegisterForLaunch(get())
        }
        scoped<PermissionsChecker> {
            ActivityPermissionsChecker(get())
        }
        scoped<IFaceTrackingPermissions> {
            FaceTrackingPermissions(get(), androidContext().packageName, get(), get(), get())
        }
        viewModel { FaceTrackingViewModel(get()) }
        viewModel { SelectionModeViewModel(get()) }
    }

    single<IdlingResourceContainer>(named<SplashViewModel>()) { IdlingResourceContainerImpl() }
    single<IdlingResourceContainer>(named<PresetsViewModel>()) { IdlingResourceContainerImpl() }
    single { VocableSharedPreferences() } bind IVocableSharedPreferences::class
    single {
        LegacyCategoriesAndPhrasesRepository(
            get(),
            get()
        )
    } bind ILegacyCategoriesAndPhrasesRepository::class
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single { LocalizedResourceUtility(androidContext()) } bind ILocalizedResourceUtility::class
    single { CategoriesUseCase(get(), get(), get(), get(), get()) } bind ICategoriesUseCase::class
    single { PhrasesUseCase(get(), get(), get(), get(), get(), get()) } bind IPhrasesUseCase::class
    single { RandomUUIDProvider() } bind UUIDProvider::class
    single { JavaDateProvider() } bind DateProvider::class
    single { JavaLocaleProvider() } bind LocaleProvider::class
    single { RoomStoredCategoriesRepository(get()) } bind StoredCategoriesRepository::class
    single { RoomPresetCategoriesRepository(get()) } bind PresetCategoriesRepository::class
    single { RoomStoredPhrasesRepository(get(), get()) } bind StoredPhrasesRepository::class
    single { RoomPresetPhrasesRepository(get(), get()) } bind PresetPhrasesRepository::class
    single { VocableDatabase.createVocableDatabase(get()) }
    single { get<VocableDatabase>().presetPhrasesDao() }
    single<VocableEnvironment> { VocableEnvironmentImpl() }
    viewModel { PresetsViewModel(get(), get(), get(named<PresetsViewModel>()), get()) }
    viewModel { EditCategoriesViewModel(get()) }
    viewModel { EditCategoryPhrasesViewModel(get(), get(), get()) }
    viewModel { AddUpdateCategoryViewModel(get(), get(), get()) }
    viewModel { EditCategoryMenuViewModel(get()) }
    viewModel { CustomCategoryPhraseViewModel(get()) }
}