package com.willowtree.vocable.ui.selectionmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.isEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel for the Selection Mode screen. */
class SelectionModeViewModel(
    private val faceTrackingPermissions: IFaceTrackingPermissions,
    private val sharedPreferences: IVocableSharedPreferences
) : ViewModel() {

    val headTrackingEnabled = faceTrackingPermissions.permissionState.asLiveData().map { it.isEnabled() }

    private val _selectedVoiceLabel = MutableStateFlow(
        sharedPreferences.getSelectedVoiceName() ?: DEFAULT_VOICE_LABEL
    )
    val selectedVoiceLabel: StateFlow<String> = _selectedVoiceLabel.asStateFlow()

    fun refreshVoiceLabel() {
        _selectedVoiceLabel.value = sharedPreferences.getSelectedVoiceName() ?: DEFAULT_VOICE_LABEL
    }

    fun requestHeadTracking() {
        faceTrackingPermissions.requestFaceTracking()
    }

    fun disableHeadTracking() {
        faceTrackingPermissions.disableFaceTracking()
    }

    companion object {
        const val DEFAULT_VOICE_LABEL = "Default"
    }
}
