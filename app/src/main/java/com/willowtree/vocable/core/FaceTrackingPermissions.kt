package com.willowtree.vocable.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.willowtree.vocable.core.permissions.PermissionRequestLauncher
import com.willowtree.vocable.core.permissions.PermissionRequester
import com.willowtree.vocable.core.permissions.PermissionsChecker
import com.willowtree.vocable.core.permissions.PermissionsRationaleDialogShower
import kotlinx.coroutines.flow.MutableStateFlow

class FaceTrackingPermissions(
    private val sharedPreferences: IVocableSharedPreferences,
    private val packageName: String,
    private val hasPermissionsChecker: PermissionsChecker,
    private val permissionsRationaleDialogShower: PermissionsRationaleDialogShower,
    permissionRequester: PermissionRequester,
) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (sharedPreferences.getHeadTrackingEnabled()) IFaceTrackingPermissions.PermissionState.Enabled else IFaceTrackingPermissions.PermissionState.Disabled)

    private val permissionLauncher: PermissionRequestLauncher =
        permissionRequester.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableFaceTracking()
            } else {
                disableFaceTracking()
                showSettingsPermissionRationaleDialog()
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
            } else {
                disableFaceTracking()
            }
        }

    init {
        // We check for permissions on startup, if we have them or receive them `permissionState` will be updated
        if (sharedPreferences.getHeadTrackingEnabled() && permissionState.value != IFaceTrackingPermissions.PermissionState.Enabled) {
            requestFaceTracking()
        }
    }

    private fun enableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(true)
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Enabled)
    }

    override fun disableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(false)
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
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
        permissionsRationaleDialogShower.showPermissionRationaleDialog(
            onPositiveClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onNegativeClick = ::disableFaceTracking,
            onDismiss = ::disableFaceTracking,
        )
    }

    private fun showSettingsPermissionRationaleDialog() {
        permissionsRationaleDialogShower.showSettingsPermissionRationaleDialog(
            onPositiveClick = {
                permissionRequestViaSettingsLauncher.launch(Manifest.permission.CAMERA)
            },
            onNegativeClick = ::disableFaceTracking,
        )
    }
}