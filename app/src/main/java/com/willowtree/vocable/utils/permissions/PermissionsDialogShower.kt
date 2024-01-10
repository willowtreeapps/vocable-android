package com.willowtree.vocable.utils.permissions

interface PermissionsDialogShower {

    fun showPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    )

    fun showSettingsPermissionDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    )
}