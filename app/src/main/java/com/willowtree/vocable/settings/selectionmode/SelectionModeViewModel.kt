package com.willowtree.vocable.settings.selectionmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.willowtree.vocable.utils.FaceTrackingPermissions
import com.willowtree.vocable.utils.IFaceTrackingPermissions
import com.willowtree.vocable.utils.isEnabled

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


