package com.willowtree.vocable.ui.sensitivity

import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SensitivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        dwellTime: Long = 3000L,
        sensitivity: Float = SensitivityViewModel.HIGH_SENSITIVITY,
        prefs: FakeVocableSharedPreferences = FakeVocableSharedPreferences(dwellTime = dwellTime, sensitivity = sensitivity)
    ): SensitivityViewModel = SensitivityViewModel(prefs)

    @Test
    fun `initial state reflects stored dwell time and sensitivity`() {
        val viewModel = createViewModel(dwellTime = 2500L, sensitivity = SensitivityViewModel.LOW_SENSITIVITY)

        assertEquals(2500L, viewModel.dwellTime.value)
        assertEquals(SensitivityViewModel.LOW_SENSITIVITY, viewModel.sensitivity.value)
    }

    @Test
    fun `requestReset opens the reset dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestReset()

        assertTrue(viewModel.isResetDialogOpen.value)
    }

    @Test
    fun `dismissResetDialog after requestReset makes no changes`() = runTest {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L, sensitivity = SensitivityViewModel.HIGH_SENSITIVITY)
        val viewModel = createViewModel(prefs = prefs)
        viewModel.requestReset()

        viewModel.dismissResetDialog()

        assertFalse(viewModel.isResetDialogOpen.value)
        assertEquals(3000L, viewModel.dwellTime.value)
        assertEquals(SensitivityViewModel.HIGH_SENSITIVITY, viewModel.sensitivity.value)
    }

    @Test
    fun `confirmReset resets dwell time and sensitivity to defaults and closes the dialog`() = runTest {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L, sensitivity = SensitivityViewModel.HIGH_SENSITIVITY)
        val viewModel = createViewModel(prefs = prefs)
        viewModel.requestReset()

        viewModel.confirmReset()

        assertFalse(viewModel.isResetDialogOpen.value)
        assertEquals(SensitivityViewModel.DWELL_TIME_ONE_SECOND, viewModel.dwellTime.value)
        assertEquals(SensitivityViewModel.MEDIUM_SENSITIVITY, viewModel.sensitivity.value)
        assertEquals(SensitivityViewModel.DWELL_TIME_ONE_SECOND, prefs.getDwellTime())
        assertEquals(SensitivityViewModel.MEDIUM_SENSITIVITY, prefs.getSensitivity())
    }
}
