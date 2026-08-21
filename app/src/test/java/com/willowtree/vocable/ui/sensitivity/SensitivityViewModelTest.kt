package com.willowtree.vocable.ui.sensitivity

import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import org.junit.Assert.assertEquals
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

}
