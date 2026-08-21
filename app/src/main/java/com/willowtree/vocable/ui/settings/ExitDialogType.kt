package com.willowtree.vocable.ui.settings

/** Types of confirmation dialogs the Settings screen can show. */
enum class ExitDialogType {
    /** Dialog shown when the user taps the Privacy Policy link, since it leaves the app. */
    PRIVACY_POLICY,
    /** Dialog shown when the user taps Contact Developers, since it leaves the app. */
    CONTACT_DEVELOPERS,
    /** No dialog should be shown. */
    NONE
}