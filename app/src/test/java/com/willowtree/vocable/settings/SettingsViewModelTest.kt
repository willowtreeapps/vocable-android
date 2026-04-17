package com.willowtree.vocable.settings

import app.cash.turbine.test
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.ui.settings.ExitDialogType
import com.willowtree.vocable.ui.settings.SettingsEvent
import com.willowtree.vocable.ui.settings.SettingsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(): SettingsViewModel = SettingsViewModel()

    @Test
    fun `onEditCategories emits navigate event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onEditCategories()
            assertEquals(SettingsEvent.NavigateToEditCategories, awaitItem())
        }
    }

    @Test
    fun `onSelectionMode emits navigate event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onSelectionMode()
            assertEquals(SettingsEvent.NavigateToSelectionMode, awaitItem())
        }
    }

    @Test
    fun `requestPrivacyPolicy updates dialog state`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestPrivacyPolicy()

        assertEquals(ExitDialogType.PRIVACY_POLICY, viewModel.uiState.value.dialogType)
    }

    @Test
    fun `dismissDialog clears dialog state`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestContactDevs()
        viewModel.dismissDialog()

        assertEquals(ExitDialogType.NONE, viewModel.uiState.value.dialogType)
    }

    @Test
    fun `confirmDialog for privacy emits url event and clears state`() = runTest {
        val viewModel = createViewModel()
        viewModel.requestPrivacyPolicy()

        viewModel.event.test {
            viewModel.confirmDialog()
            assertEquals(
                SettingsEvent.OpenPrivacyPolicy("https://vocable.app/privacy.html"),
                awaitItem()
            )
        }

        assertEquals(ExitDialogType.NONE, viewModel.uiState.value.dialogType)
    }
}
