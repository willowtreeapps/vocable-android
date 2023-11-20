package com.willowtree.vocable.settings.selectionmode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.getOrAwaitValue
import com.willowtree.vocable.utils.FakeFaceTrackingPermissions
import com.willowtree.vocable.utils.IFaceTrackingPermissions
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test


class SelectionModeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun createViewModel(permissions: IFaceTrackingPermissions): SelectionModeViewModel {
        return SelectionModeViewModel(permissions)
    }

    private fun createTrackingPermissions(headTrackingEnabled: Boolean): IFaceTrackingPermissions {
        return FakeFaceTrackingPermissions(headTrackingEnabled)
    }

    @Test
    fun `headTrackingEnabled true on init if head tracking Enabled`() = runTest {

        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = true))

        assert(viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `headTrackingEnabled false on init if head tracking disabled`() = runTest {

        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = false))

        assert(!viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `requestHeadTracking() sets headTrackingEnabled to false`() = runTest {

        // Setting false so its not Requested on init
        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = false))

        assert(!viewModel.headTrackingEnabled.getOrAwaitValue())

        viewModel.requestHeadTracking()

        assert(!viewModel.headTrackingEnabled.getOrAwaitValue())
    }

    @Test
    fun `disableHeadTracking() sets headTrackingEnabled to false`() = runTest {

        // Setting true so its not Disabled on init
        val viewModel = createViewModel(createTrackingPermissions(headTrackingEnabled = true))

        assert(viewModel.headTrackingEnabled.getOrAwaitValue())

        viewModel.disableHeadTracking()

        assert(!viewModel.headTrackingEnabled.getOrAwaitValue())
    }
}