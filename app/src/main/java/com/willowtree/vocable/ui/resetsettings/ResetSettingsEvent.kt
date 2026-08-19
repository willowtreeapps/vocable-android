package com.willowtree.vocable.ui.resetsettings

/** Events that the Reset App Settings screen can send to its parent. */
sealed interface ResetSettingsEvent {
    data object NavigateBack : ResetSettingsEvent

    /** Toast feedback after a reset attempt - [success] false if a reset call threw. */
    data class ShowResetResult(val success: Boolean) : ResetSettingsEvent
}
