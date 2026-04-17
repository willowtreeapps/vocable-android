package com.willowtree.vocable.ui.settings

/** Types of dialogs that can be shown when exiting the Settings screen. */
enum class ExitDialogType {
    /** Dialog shown when the user tries to exit the Settings screen while editing categories. */
    PRIVACY_POLICY,
    /** Dialog shown when the user tries to exit the Settings screen while editing categories. */
    CONTACT_DEVELOPERS,
    /** No dialog should be shown. */
    NONE
}