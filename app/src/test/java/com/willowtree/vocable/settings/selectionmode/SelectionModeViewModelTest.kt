package com.willowtree.vocable.settings.selectionmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import com.willowtree.vocable.utils.IVocableSharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


class SelectionModeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun createViewModel(sharedPreferences: IVocableSharedPreferences): SelectionModeViewModel {
        return SelectionModeViewModel(sharedPreferences)
    }

    private fun createSharedPrefs(headTrackingEnabled: Boolean): IVocableSharedPreferences {
        return FakeVocableSharedPreferences(headTrackingEnabled = headTrackingEnabled)
    }

    @Test
    fun `permission state Requested on init if head tracking Enabled`() = runTest {

        val viewModel = createViewModel(createSharedPrefs(headTrackingEnabled = true))

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.PermissionRequested)
    }

    @Test
    fun `permission state Disabled on init if head tracking disabled`() = runTest {

        val viewModel = createViewModel(createSharedPrefs(headTrackingEnabled = false))

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.Disabled)
    }

    @Test
    fun `requestHeadTracking() sets permission state to PermissionRequested`() = runTest {

        // Setting false so its not Requested on init
        val viewModel = createViewModel(createSharedPrefs(headTrackingEnabled = false))

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.Disabled)

        viewModel.requestHeadTracking()

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.PermissionRequested)
    }

    @Test
    fun `enableHeadTracking() sets permission state to Enabled`() = runTest {

        val sharedPreference = createSharedPrefs(headTrackingEnabled = false)
        assert(!sharedPreference.getHeadTrackingEnabled())

        // Setting false so its not Requested on init
        val viewModel = createViewModel(sharedPreference)

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.Disabled)

        viewModel.enableHeadTracking()

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.Enabled)
        // Check shared preferences is updated
        assert(sharedPreference.getHeadTrackingEnabled())
    }

    @Test
    fun `disableHeadTracking() sets permission state to Disabled`() = runTest {

        val sharedPreference = createSharedPrefs(headTrackingEnabled = true)
        assert(sharedPreference.getHeadTrackingEnabled())

        // Setting true so its not Disabled on init
        val viewModel = createViewModel(sharedPreference)

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.PermissionRequested)

        viewModel.disableHeadTracking()

        assert(viewModel.headTrackingPermissionState.getOrAwaitValue() is HeadTrackingPermissionState.Disabled)
        // Check shared preferences is updated
        assert(!sharedPreference.getHeadTrackingEnabled())
    }
}