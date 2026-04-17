package com.willowtree.vocable.ui.settings

/** Events that the Settings screen can send to its parent. */
sealed interface SettingsEvent {
    data object NavigateToEditCategories : SettingsEvent
    data object NavigateToTimingSensitivity : SettingsEvent
    data object NavigateToSelectionMode : SettingsEvent
    data class OpenPrivacyPolicy(val url: String) : SettingsEvent
    data class ContactDevelopers(val mailTo: String) : SettingsEvent
}