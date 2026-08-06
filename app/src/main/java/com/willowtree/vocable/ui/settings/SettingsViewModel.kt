package com.willowtree.vocable.ui.settings

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.launch

/** ViewModel for the [SettingsScreen]. */
class SettingsViewModel(
    private val prefs: IVocableSharedPreferences,
    private val categoriesUseCase: ICategoriesUseCase
) : BaseViewModel<SettingsState, SettingsEvent>(SettingsState(selectedVoiceLabel = prefs.getSelectedVoiceName())) {

    fun onEditCategories() {
        sendEvent(SettingsEvent.NavigateToEditCategories)
    }

    fun onTimingSensitivity() {
        sendEvent(SettingsEvent.NavigateToTimingSensitivity)
    }

    fun onSelectionMode() {
        sendEvent(SettingsEvent.NavigateToSelectionMode)
    }

    fun onVoiceSelection() {
        sendEvent(SettingsEvent.NavigateToVoiceSelection)
    }

    fun requestPrivacyPolicy() {
        updateState { copy(dialogType = ExitDialogType.PRIVACY_POLICY) }
    }

    fun requestContactDevs() {
        updateState { copy(dialogType = ExitDialogType.CONTACT_DEVELOPERS) }
    }

    fun requestReset() {
        updateState { copy(dialogType = ExitDialogType.RESET_APP_SETTINGS) }
    }

    fun dismissDialog() {
        updateState { copy(dialogType = ExitDialogType.NONE) }
    }

    fun confirmDialog() {
        val currentType = uiState.value.dialogType
        dismissDialog()
        when (currentType) {
            ExitDialogType.PRIVACY_POLICY -> {
                sendEvent(SettingsEvent.OpenPrivacyPolicy(PRIVACY_POLICY))
            }
            ExitDialogType.CONTACT_DEVELOPERS -> {
                val versionSuffix = "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"
                sendEvent(SettingsEvent.ContactDevelopers(MAIL_TO + versionSuffix))
            }
            ExitDialogType.RESET_APP_SETTINGS -> {
                viewModelScope.launch {
                    categoriesUseCase.resetToDefaults()
                    prefs.clearAll()
                }
            }
            ExitDialogType.NONE -> {
                // Do nothing
            }
        }
    }

    companion object {
        private const val PRIVACY_POLICY = "https://vocable.app/privacy.html"
        private const val MAIL_TO = "mailto:vocable@willowtreeapps.com?subject=Feedback for Android Vocable "
    }
}