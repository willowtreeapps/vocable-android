package com.willowtree.vocable.utils

import kotlinx.coroutines.flow.MutableStateFlow


class FakeFaceTrackingPermissions(enabled: Boolean) : IFaceTrackingPermissions {

    override val permissionState: MutableStateFlow<IFaceTrackingPermissions.PermissionState> =
        MutableStateFlow(if (enabled) IFaceTrackingPermissions.PermissionState.Enabled else IFaceTrackingPermissions.PermissionState.Disabled)

    override fun initialize() {
        // no-op
    }

    override fun requestFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.PermissionRequested)
    }

    override fun disableFaceTracking() {
        permissionState.tryEmit(IFaceTrackingPermissions.PermissionState.Disabled)
    }
}