package com.willowtree.vocable.ui.resetsettings

/** State for the Reset App Settings screen. */
data class ResetSettingsState(
    val checkedDomains: Set<ResetDomain> = emptySet(),
    val dialogTarget: ResetDialogTarget? = null
)

/** Which confirmation dialog is currently open, if any. */
sealed interface ResetDialogTarget {
    /** Reset only the domains currently checked. */
    data object Selected : ResetDialogTarget

    /** The nuclear option - reset the entire app to defaults. */
    data object Everything : ResetDialogTarget
}
