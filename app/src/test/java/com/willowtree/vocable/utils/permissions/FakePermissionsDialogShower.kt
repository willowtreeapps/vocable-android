package com.willowtree.vocable.utils.permissions

class FakePermissionsDialogShower : PermissionsDialogShower {

    var showPermissionRationaleDialogCalledCount = 0
        private set

    var showSettingsPermissionDialogCalledCount = 0
        private set

    var permissionDialogOnPositiveClick : () -> Unit = {}
        private set

    var permissionDialogOnNegativeClick : () -> Unit = {}
        private set

    var settingsPermissionsDialogOnPositiveClick : () -> Unit = {}
        private set

    override fun showPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        showPermissionRationaleDialogCalledCount++
        permissionDialogOnPositiveClick = onPositiveClick
        permissionDialogOnNegativeClick = onNegativeClick
    }

    override fun showSettingsPermissionDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    ) {
        showSettingsPermissionDialogCalledCount++
        settingsPermissionsDialogOnPositiveClick = onPositiveClick
    }
}