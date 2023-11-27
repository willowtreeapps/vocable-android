package com.willowtree.vocable.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.willowtree.vocable.R

class CameraPermissionsHelper(private val activity: AppCompatActivity) {

    var permissionCallback: ((Boolean) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permissionCallback?.invoke(isGranted)
            if (!isGranted) {
                permissionSettingsDialog.show()
            }
        }

    private val permissionResultLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(object : ActivityResultContract<String, Boolean>() {
            override fun createIntent(context: Context, input: String): Intent {
                return Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                return when (resultCode) {
                    Activity.RESULT_OK, Activity.RESULT_CANCELED -> true
                    else -> false
                }
            }
        }) { result ->
            if (result) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

    private val permissionRationaleDialog by lazy {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_rationale_title))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { _, _ ->
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(activity.getString(R.string.permissions_cancel)) { dialog, _ ->
                permissionCallback?.invoke(false)
                dialog.dismiss()
            }
            .setOnDismissListener { dialog ->
                permissionCallback?.invoke(false)
                dialog.dismiss()
            }.create()
    }

    private val permissionSettingsDialog by lazy {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_missing_dialog_title))
            .setMessage(activity.getString(R.string.permissions_missing_dialog_body))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { _, _ ->
                permissionResultLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(activity.getString(R.string.permissions_cancel)) { dialog, _ ->
                permissionCallback?.invoke(false)
                dialog.dismiss()
            }
            .create()
    }

    fun requestPermissions() {

        // Bypass check if we already have permission
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionCallback?.invoke(true)
            return
        }

        // Permission has been denied before, show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            permissionRationaleDialog.show()
            return
        }

        // Ask for permissions. We are showing rationale here as a primer
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}