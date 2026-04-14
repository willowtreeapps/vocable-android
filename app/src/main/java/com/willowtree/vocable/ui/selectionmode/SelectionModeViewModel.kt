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

    private val _selectedLanguageLabel = MutableStateFlow(
        resolveLanguageLabel(sharedPreferences.getSelectedLanguageTag())
    )
    val selectedLanguageLabel: StateFlow<String> = _selectedLanguageLabel.asStateFlow()

    fun refreshLabels() {
        _selectedVoiceLabel.value = sharedPreferences.getSelectedVoiceName() ?: DEFAULT_VOICE_LABEL
        _selectedLanguageLabel.value = resolveLanguageLabel(sharedPreferences.getSelectedLanguageTag())
    }

    fun requestHeadTracking() {
        faceTrackingPermissions.requestFaceTracking()
    }

    fun disableHeadTracking() {
        faceTrackingPermissions.disableFaceTracking()
    }

    companion object {
        const val DEFAULT_VOICE_LABEL = "Default"
        const val DEFAULT_LANGUAGE_LABEL = "System Default"

        private fun resolveLanguageLabel(tag: String?): String {
            if (tag.isNullOrEmpty()) return DEFAULT_LANGUAGE_LABEL
            val locale = java.util.Locale.forLanguageTag(tag)
            return locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
        }
    }
}
