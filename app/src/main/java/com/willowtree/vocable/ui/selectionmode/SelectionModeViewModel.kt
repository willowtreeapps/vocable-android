package com.willowtree.vocable.ui.selectionmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.isEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** ViewModel for the Selection Mode screen. */
class SelectionModeViewModel(
    private val faceTrackingPermissions: IFaceTrackingPermissions
) : ViewModel() {

    val headTrackingEnabled = faceTrackingPermissions.permissionState.asLiveData().map { it.isEnabled() }

    private val _isResetDialogOpen = MutableStateFlow(false)
    val isResetDialogOpen: StateFlow<Boolean> = _isResetDialogOpen.asStateFlow()

    fun requestHeadTracking() {
        faceTrackingPermissions.requestFaceTracking()
    }

    fun disableHeadTracking() {
        faceTrackingPermissions.disableFaceTracking()
    }

    fun requestReset() {
        _isResetDialogOpen.update { true }
    }

    fun dismissResetDialog() {
        _isResetDialogOpen.update { false }
    }

    fun confirmReset() {
        _isResetDialogOpen.update { false }
        faceTrackingPermissions.resetToDefault()
    }
}
