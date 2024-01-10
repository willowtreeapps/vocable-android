package com.willowtree.vocable.utils.permissions

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.willowtree.vocable.R

class ActivityPermissionsDialogShower(
    private val activity: AppCompatActivity,
) : PermissionsDialogShower {

    override fun showPermissionRationaleDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_rationale_title))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { dialog, _ ->
                onPositiveClick()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.settings_dialog_cancel)) { dialog, _ ->
                onNegativeClick()
                dialog.dismiss()
            }
            .setOnDismissListener { dialog ->
                onDismiss()
                dialog.dismiss()
            }.create().show()

    }

    override fun showSettingsPermissionDialog(
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit,
    ) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_missing_dialog_title))
            .setMessage(activity.getString(R.string.permissions_missing_dialog_body))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { dialog, _ ->
                onPositiveClick()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.settings_dialog_cancel)) { dialog, _ ->
                onNegativeClick()
                dialog.dismiss()
            }
            .setOnDismissListener { dialog ->
                onNegativeClick()
                dialog.dismiss()
            }
            .create().show()
    }
}