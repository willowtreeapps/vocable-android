package com.willowtree.vocable.settings.selectionmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.ui.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.utils.FakeFaceTrackingPermissions
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import com.willowtree.vocable.core.IFaceTrackingPermissions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SelectionModeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun createViewModel(permissions: IFaceTrackingPermissions): SelectionModeViewModel {
        return SelectionModeViewModel(permissions, FakeVocableSharedPreferences())
    }

    private fun createTrackingPermissions(headTrackingEnabled: Boolean): FakeFaceTrackingPermissions {
        return FakeFaceTrackingPermissions(headTrackingEnabled)
    }

    @Test
    fun `headTrackingEnabled true on init if head tracking Enabled`() = runTest {
        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = true))
        assertTrue(viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `headTrackingEnabled false on init if head tracking disabled`() = runTest {
        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = false))
        assertFalse(viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `requestHeadTracking delegates to permissions`() = runTest {
        val permissions = createTrackingPermissions(headTrackingEnabled = false)
        val viewModel = createViewModel(permissions)

        assertFalse(viewModel.headTrackingEnabled.getOrAwaitValue())
        viewModel.requestHeadTracking()

        assertTrue(permissions.requestFaceTrackingCalled)
        assertFalse(viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `disableHeadTracking sets headTrackingEnabled to false`() = runTest {
        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = true))
        assertTrue(viewModel.headTrackingEnabled.getOrAwaitValue())
        viewModel.disableHeadTracking()
        assertFalse(viewModel.headTrackingEnabled.getOrAwaitValue())
    }
}
