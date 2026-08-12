package com.willowtree.vocable.ui.resetsettings

/** Events that the Reset App Settings screen can send to its parent. */
sealed interface ResetSettingsEvent {
    data object NavigateBack : ResetSettingsEvent
}
