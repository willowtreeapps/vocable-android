package com.willowtree.vocable.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.core.DateProvider
import com.willowtree.vocable.core.FaceTrackingManager
import com.willowtree.vocable.core.FaceTrackingPermissions
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.IdlingResourceContainer
import com.willowtree.vocable.core.IdlingResourceContainerImpl
import com.willowtree.vocable.core.JavaDateProvider
import com.willowtree.vocable.core.RandomUUIDProvider
import com.willowtree.vocable.core.UUIDProvider
import com.willowtree.vocable.core.VocableEnvironment
import com.willowtree.vocable.core.VocableEnvironmentImpl
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.locale.JavaLocaleProvider
import com.willowtree.vocable.core.locale.LocaleProvider
import com.willowtree.vocable.core.locale.LocalizedResourceUtility
import com.willowtree.vocable.core.permissions.ActivityPermissionRegisterForLaunch
import com.willowtree.vocable.core.permissions.ActivityPermissionsChecker
import com.willowtree.vocable.core.permissions.ActivityPermissionsRationaleDialogShower
import com.willowtree.vocable.core.permissions.PermissionRequester
import com.willowtree.vocable.core.permissions.PermissionsChecker
import com.willowtree.vocable.core.permissions.PermissionsRationaleDialogShower
import com.willowtree.vocable.data.repository.PresetCategoriesRepository
import com.willowtree.vocable.data.repository.PresetPhrasesRepository
import com.willowtree.vocable.data.repository.RoomPresetCategoriesRepository
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.data.repository.RoomStoredCategoriesRepository
import com.willowtree.vocable.data.repository.RoomStoredPhrasesRepository
import com.willowtree.vocable.data.repository.StoredCategoriesRepository
import com.willowtree.vocable.data.repository.StoredPhrasesRepository
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.domain.usecase.CategoriesUseCase
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.domain.usecase.PhrasesUseCase
import com.willowtree.vocable.ui.editcategories.EditCategoriesViewModel
import com.willowtree.vocable.ui.editcategorymenu.EditCategoryMenuViewModel
import com.willowtree.vocable.ui.editphrases.EditCategoryPhrasesViewModel
import com.willowtree.vocable.ui.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.ui.keyboard.KeyboardViewModel
import com.willowtree.vocable.ui.presets.PresetsViewModel
import com.willowtree.vocable.ui.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.ui.settings.SettingsViewModel
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel
import com.willowtree.vocable.ui.splash.SplashActivity
import com.willowtree.vocable.ui.splash.SplashViewModel
import com.willowtree.vocable.ui.languageselection.LanguageSelectionViewModel
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val vocableKoinModule = module {

    scope<SplashActivity> {
        viewModel { SplashViewModel(get(), get(), get(named<SplashViewModel>())) }
    }

    scope<MainActivity> {
        scoped<PermissionsRationaleDialogShower> { ActivityPermissionsRationaleDialogShower(get()) }
        scoped<PermissionRequester> { ActivityPermissionRegisterForLaunch(get()) }
        scoped<PermissionsChecker> { ActivityPermissionsChecker(get()) }

        scoped<IFaceTrackingPermissions> {
            FaceTrackingPermissions(
                sharedPreferences = get(),
                packageName = androidContext().packageName,
                hasPermissionsChecker = get(),
                permissionsRationaleDialogShower = get(),
                permissionRequester = get()
            )
        }

        scoped { FaceTrackingManager(get(), get()) }
        viewModel { FaceTrackingViewModel(get()) }
        viewModel { SelectionModeViewModel(get(), get()) }
    }

    factory<IFaceTrackingPermissions> {
        object : IFaceTrackingPermissions {
            override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
                MutableStateFlow(IFaceTrackingPermissions.PermissionState.Disabled)

            override fun disableFaceTracking() = Unit
            override fun requestFaceTracking() = Unit
        }
    }

    single<IdlingResourceContainer>(named<SplashViewModel>()) { IdlingResourceContainerImpl() }
    single<IdlingResourceContainer>(named<PresetsViewModel>()) { IdlingResourceContainerImpl() }
    single { VocableSharedPreferences() } bind IVocableSharedPreferences::class
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single { LocalizedResourceUtility(androidContext()) } bind ILocalizedResourceUtility::class
    single { CategoriesUseCase(get(), get(), get(), get(), get()) } bind ICategoriesUseCase::class
    single { PhrasesUseCase(get(), get(), get(), get(), get()) } bind IPhrasesUseCase::class
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

    viewModel { PresetsViewModel(get(), get(), get(named<PresetsViewModel>()), get(), get()) }
    viewModel { EditCategoriesViewModel(get()) }
    viewModel { EditCategoryPhrasesViewModel(get(), get(), get()) }
    viewModel { EditCategoryMenuViewModel(get()) }
    viewModel { KeyboardViewModel() }
    viewModel { SettingsViewModel(get()) }
    viewModel { SensitivityViewModel(get()) }
    viewModel { VoiceSelectionViewModel(get()) }
    viewModel { LanguageSelectionViewModel(get()) }
}