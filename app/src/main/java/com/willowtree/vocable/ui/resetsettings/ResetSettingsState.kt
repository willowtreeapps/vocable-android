package com.willowtree.vocable.ui.resetsettings

import kotlin.math.ceil

/** State for the Reset App Settings screen. */
data class ResetSettingsState(
    val checkedDomains: Set<ResetDomain> = emptySet(),
    val dialogTarget: ResetDialogTarget? = null,
    val currentPage: Int = 0,
    /** Defaults to showing every domain on one page; the screen measures and lowers this only if it doesn't fit. */
    val itemsPerPage: Int = ResetDomain.entries.size
) {
    val totalPages: Int
        get() = ceil(ResetDomain.entries.size.toFloat() / itemsPerPage).toInt().coerceAtLeast(1)
}

/** Which confirmation dialog is currently open, if any. */
sealed interface ResetDialogTarget {
    /** Reset only the domains currently checked. */
    data object Selected : ResetDialogTarget

    /** The nuclear option - reset the entire app to defaults. */
    data object Everything : ResetDialogTarget
}
