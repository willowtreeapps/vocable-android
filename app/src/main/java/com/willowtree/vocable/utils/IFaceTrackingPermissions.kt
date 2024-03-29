package com.willowtree.vocable.utils

import kotlinx.coroutines.flow.MutableStateFlow


interface IFaceTrackingPermissions {

    sealed interface PermissionState {
        object Enabled : PermissionState
        object Disabled : PermissionState
    }

    val permissionState: MutableStateFlow<PermissionState>

    fun requestFaceTracking()

    fun disableFaceTracking()
}

fun IFaceTrackingPermissions.PermissionState.isEnabled() = this == IFaceTrackingPermissions.PermissionState.Enabled