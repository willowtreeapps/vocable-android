package com.willowtree.vocable.utils

import kotlinx.coroutines.flow.MutableStateFlow


class FakeFaceTrackingPermissions(enabled: Boolean) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (enabled) IFaceTrackingPermissions.PermissionState.Enabled else IFaceTrackingPermissions.PermissionState.Disabled)

    override fun requestFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.PermissionRequested)
    }

    override fun enableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Enabled)
    }

    override fun disableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
    }
}