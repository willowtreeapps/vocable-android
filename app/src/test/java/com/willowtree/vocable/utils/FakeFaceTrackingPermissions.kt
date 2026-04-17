package com.willowtree.vocable.utils

import com.willowtree.vocable.core.IFaceTrackingPermissions
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFaceTrackingPermissions(enabled: Boolean) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (enabled) IFaceTrackingPermissions.PermissionState.Enabled else IFaceTrackingPermissions.PermissionState.Disabled)

    var requestFaceTrackingCalled: Boolean = false
        private set

    override fun requestFaceTracking() {
        requestFaceTrackingCalled = true
    }

    override fun disableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
    }
}
