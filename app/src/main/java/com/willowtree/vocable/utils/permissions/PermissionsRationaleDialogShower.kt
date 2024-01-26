package com.willowtree.vocable.utils.permissions

interface PermissionsRationaleDialogShower {

    fun showPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    )

    fun showSettingsPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    )
}