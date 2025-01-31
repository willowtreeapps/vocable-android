package com.willowtree.vocable.utility

import com.willowtree.vocable.utils.IFaceTrackingPermissions
import kotlinx.coroutines.flow.MutableStateFlow

class MockFaceTrackingManager {
    fun startFaceTracking() {}
    fun stopFaceTracking() {}
}

class MockFaceTrackingPermissions : IFaceTrackingPermissions {
    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(IFaceTrackingPermissions.PermissionState.Disabled)

    override fun requestFaceTracking() {}

    override fun disableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
    }
} 
