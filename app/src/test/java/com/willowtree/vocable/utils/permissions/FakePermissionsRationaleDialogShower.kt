package com.willowtree.vocable.utils.permissions

class FakePermissionsRationaleDialogShower : PermissionsRationaleDialogShower {

    var rationaleDialogShowedCount = 0
        private set

    var settingsRationaleDialogShowedCount = 0
        private set

    var rationaleDialogOnPositiveClick : () -> Unit = {}
        private set

    var rationaleDialogOnNegativeClick : () -> Unit = {}
        private set

    var settingsRationaleDialogOnPositiveClick : () -> Unit = {}
        private set

    override fun showPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        rationaleDialogShowedCount++
        rationaleDialogOnPositiveClick = onPositiveClick
        rationaleDialogOnNegativeClick = onNegativeClick
    }

    override fun showSettingsPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    ) {
        settingsRationaleDialogShowedCount++
        settingsRationaleDialogOnPositiveClick = onPositiveClick
    }
}