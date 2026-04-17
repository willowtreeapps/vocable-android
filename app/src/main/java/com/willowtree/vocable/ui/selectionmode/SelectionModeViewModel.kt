package com.willowtree.vocable.ui.selectionmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.isEnabled

/** ViewModel for the Selection Mode screen. */
class SelectionModeViewModel(
    private val faceTrackingPermissions: IFaceTrackingPermissions,
) : ViewModel() {

    val headTrackingEnabled = faceTrackingPermissions.permissionState.asLiveData().map { it.isEnabled() }

    fun requestHeadTracking() {
        faceTrackingPermissions.requestFaceTracking()
    }

    fun disableHeadTracking() {
        faceTrackingPermissions.disableFaceTracking()
    }

}


