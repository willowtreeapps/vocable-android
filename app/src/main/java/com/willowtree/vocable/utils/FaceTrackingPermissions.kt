package com.willowtree.vocable.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.Disabled
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.Enabled
import com.willowtree.vocable.utils.permissions.PermissionRequestLauncher
import com.willowtree.vocable.utils.permissions.PermissionRequester
import com.willowtree.vocable.utils.permissions.PermissionsChecker
import com.willowtree.vocable.utils.permissions.PermissionsDialogShower
import kotlinx.coroutines.flow.MutableStateFlow

class FaceTrackingPermissions(
    private val sharedPreferences: IVocableSharedPreferences,
    private val packageName: String,
    private val hasPermissionsChecker: PermissionsChecker,
    private val permissionsDialogShower: PermissionsDialogShower,
    permissionRequester: PermissionRequester,
) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (sharedPreferences.getHeadTrackingEnabled()) Enabled else Disabled)

    private val permissionLauncher: PermissionRequestLauncher =
        permissionRequester.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableFaceTracking()
            } else {
                disableFaceTracking()
                showRequestPermissionsDialogFromSettings()
            }
        }

    private val permissionRequestViaSettingsLauncher: PermissionRequestLauncher =
        permissionRequester.registerForActivityResult(object : ActivityResultContract<String, Boolean>() {
            override fun createIntent(context: Context, input: String): Intent {
                return Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                return when (resultCode) {
                    Activity.RESULT_OK, Activity.RESULT_CANCELED -> true
                    else -> false
                }
            }
        }) { isGranted ->
            if (isGranted) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

    init {
        // We check for permissions on startup, if we have them or receive them `permissionState` will be updated
        if (sharedPreferences.getHeadTrackingEnabled()) {
            requestFaceTracking()
        }
    }

    private fun enableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(true)
        permissionState.tryEmit(Enabled)
    }

    override fun disableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(false)
        permissionState.tryEmit(Disabled)
    }

    override fun requestFaceTracking() {
        // Bypass check if we already have permission
        if (hasPermissionsChecker.hasPermissions(Manifest.permission.CAMERA)) {
            enableFaceTracking()
            return
        }

        // Permission has been denied before, show rationale
        if (hasPermissionsChecker.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionsRationaleDialog()
            return
        }

        // Ask for permissions. We are showing rationale here as a primer
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionsRationaleDialog() {
        permissionsDialogShower.showPermissionRationaleDialog(
            onPositiveClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onNegativeClick = ::disableFaceTracking,
            onDismiss = ::disableFaceTracking,
        )
    }

    private fun showRequestPermissionsDialogFromSettings() {
        permissionsDialogShower.showSettingsPermissionDialog(
            onPositiveClick = {
                permissionRequestViaSettingsLauncher.launch(Manifest.permission.CAMERA)
            },
            onNegativeClick = ::disableFaceTracking,
        )
    }
}