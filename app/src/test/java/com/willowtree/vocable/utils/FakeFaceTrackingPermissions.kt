package com.willowtree.vocable.utils

import kotlinx.coroutines.flow.MutableStateFlow


class FakeFaceTrackingPermissions(enabled: Boolean) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (enabled) IFaceTrackingPermissions.PermissionState.Enabled else IFaceTrackingPermissions.PermissionState.Disabled)

    override fun requestFaceTracking() {}

    override fun disableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
    }
}