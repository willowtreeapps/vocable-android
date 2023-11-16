package com.willowtree.vocable.settings.selectionmode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectionModeViewModel(
    private val sharedPrefs: VocableSharedPreferences
) : ViewModel() {

    private val liveHeadTrackingPermissionState: MutableLiveData<HeadTrackingPermissionState> = MutableLiveData()
    val headTrackingPermissionState: LiveData<HeadTrackingPermissionState> = liveHeadTrackingPermissionState

    init {
        val headTrackingEnabled = sharedPrefs.getHeadTrackingEnabled()
        // We check for permissions on startup, if we have them or receive them `liveHeadTrackingEnabled` will be updated
        liveHeadTrackingPermissionState.postValue(if (headTrackingEnabled) HeadTrackingPermissionState.PermissionRequested else HeadTrackingPermissionState.Disabled)
    }

    fun requestHeadTracking() {
        liveHeadTrackingPermissionState.postValue(HeadTrackingPermissionState.PermissionRequested)
    }

    fun enableHeadTracking() {
        sharedPrefs.setHeadTrackingEnabled(true)
        liveHeadTrackingPermissionState.postValue(HeadTrackingPermissionState.Enabled)
    }

    fun disableHeadTracking() {
        sharedPrefs.setHeadTrackingEnabled(false)
        liveHeadTrackingPermissionState.postValue(HeadTrackingPermissionState.Disabled)
    }
}

sealed interface HeadTrackingPermissionState {
    object PermissionRequested : HeadTrackingPermissionState
    object Enabled : HeadTrackingPermissionState
    object Disabled : HeadTrackingPermissionState
}
