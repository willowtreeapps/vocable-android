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
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.Disabled
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.Enabled
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.PermissionRequested
import kotlinx.coroutines.flow.MutableStateFlow


class FaceTrackingPermissions(
    private val sharedPreferences: IVocableSharedPreferences,
    private val activity: AppCompatActivity
) : IFaceTrackingPermissions {

   override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        // We check for permissions on startup, if we have them or receive them `permissionState` will be updated
        MutableStateFlow(if (sharedPreferences.getHeadTrackingEnabled()) PermissionRequested else Disabled)

    override fun requestFaceTracking() {
        permissionState.tryEmit(PermissionRequested)
        requestPermissions()
    }

    override fun enableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(true)
        permissionState.tryEmit(Enabled)
    }

    override fun disableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(false)
        permissionState.tryEmit(Disabled)
    }

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableFaceTracking()
            } else {
                disableFaceTracking()
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
            .setNegativeButton(activity.getString(R.string.settings_dialog_cancel)) { dialog, _ ->
                disableFaceTracking()
                dialog.dismiss()
            }
            .setOnDismissListener { dialog ->
                disableFaceTracking()
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
            .setNegativeButton(activity.getString(R.string.settings_dialog_cancel)) { dialog, _ ->
                disableFaceTracking()
                dialog.dismiss()
            }
            .create()
    }

    private fun requestPermissions() {
        // Bypass check if we already have permission
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            enableFaceTracking()
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