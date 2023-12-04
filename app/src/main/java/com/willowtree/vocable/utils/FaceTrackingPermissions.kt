package com.willowtree.vocable.utils

import kotlinx.coroutines.flow.MutableStateFlow
import com.willowtree.vocable.utils.IFaceTrackingPermissions.PermissionState.*


class FaceTrackingPermissions(private val sharedPreferences: IVocableSharedPreferences) : IFaceTrackingPermissions {

   override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        // We check for permissions on startup, if we have them or receive them `permissionState` will be updated
        MutableStateFlow(if (sharedPreferences.getHeadTrackingEnabled()) PermissionRequested else Disabled)

    override fun requestFaceTracking() {
        permissionState.tryEmit(PermissionRequested)
    }

    override fun enableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(true)
        permissionState.tryEmit(Enabled)
    }

    override fun disableFaceTracking() {
        sharedPreferences.setHeadTrackingEnabled(false)
        permissionState.tryEmit(Disabled)
    }
}