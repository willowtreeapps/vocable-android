package com.willowtree.vocable.utils.permissions

class FakePermissionsDialogShower : PermissionsDialogShower {

    override fun showPermissionDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        // No-op
    }

    override fun showSettingsPermissionDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    ) {
        // No-op
    }
}