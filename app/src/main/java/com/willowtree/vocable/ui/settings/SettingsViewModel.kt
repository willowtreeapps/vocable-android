package com.willowtree.vocable.ui.settings

import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.ui.base.BaseViewModel

/** ViewModel for the [SettingsScreen]. */
class SettingsViewModel : BaseViewModel<SettingsState, SettingsEvent>(SettingsState()) {

    fun onEditCategories() {
        sendEvent(SettingsEvent.NavigateToEditCategories)
    }

    fun onTimingSensitivity() {
        sendEvent(SettingsEvent.NavigateToTimingSensitivity)
    }

    fun onSelectionMode() {
        sendEvent(SettingsEvent.NavigateToSelectionMode)
    }

    fun requestPrivacyPolicy() {
        updateState { copy(dialogType = ExitDialogType.PRIVACY_POLICY) }
    }

    fun requestContactDevs() {
        updateState { copy(dialogType = ExitDialogType.CONTACT_DEVELOPERS) }
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