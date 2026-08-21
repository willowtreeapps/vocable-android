package com.willowtree.vocable.resetsettings

import app.cash.turbine.test
import com.willowtree.vocable.FakeCategoriesUseCase
import com.willowtree.vocable.FakePhrasesUseCase
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.ui.resetsettings.ResetDialogTarget
import com.willowtree.vocable.ui.resetsettings.ResetDomain
import com.willowtree.vocable.ui.resetsettings.ResetSettingsEvent
import com.willowtree.vocable.ui.resetsettings.ResetSettingsViewModel
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel
import com.willowtree.vocable.utils.FakeFaceTrackingPermissions
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResetSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        prefs: FakeVocableSharedPreferences = FakeVocableSharedPreferences(),
        categoriesUseCase: FakeCategoriesUseCase = FakeCategoriesUseCase(),
        phrasesUseCase: FakePhrasesUseCase = FakePhrasesUseCase(),
        faceTrackingPermissions: FakeFaceTrackingPermissions = FakeFaceTrackingPermissions(enabled = false)
    ): ResetSettingsViewModel = ResetSettingsViewModel(prefs, categoriesUseCase, phrasesUseCase, faceTrackingPermissions)

    @Test
    fun `onBack emits navigate back event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onBack()
            assertEquals(ResetSettingsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `toggleDomain checks and unchecks a domain`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleDomain(ResetDomain.VOICE)
        assertTrue(ResetDomain.VOICE in viewModel.uiState.value.checkedDomains)

        viewModel.toggleDomain(ResetDomain.VOICE)
        assertFalse(ResetDomain.VOICE in viewModel.uiState.value.checkedDomains)
    }

    @Test
    fun `requestResetSelected does nothing when no domains checked`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestResetSelected()

        assertNull(viewModel.uiState.value.dialogTarget)
    }

    @Test
    fun `requestResetSelected opens dialog when domains are checked`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleDomain(ResetDomain.VOICE)

        viewModel.requestResetSelected()

        assertEquals(ResetDialogTarget.Selected, viewModel.uiState.value.dialogTarget)
    }

    @Test
    fun `requestResetEverything opens dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestResetEverything()

        assertEquals(ResetDialogTarget.Everything, viewModel.uiState.value.dialogTarget)
    }

    @Test
    fun `dismissDialog after requestResetEverything makes no changes`() = runTest {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L)
        val viewModel = createViewModel(prefs = prefs)
        viewModel.requestResetEverything()

        viewModel.dismissDialog()

        assertNull(viewModel.uiState.value.dialogTarget)
        assertEquals(3000L, prefs.getDwellTime())
    }

    @Test
    fun `confirmDialog for everything wipes preferences and category data`() = runTest {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L)
        val categoriesUseCase = FakeCategoriesUseCase()
        categoriesUseCase._categories.update {
            it + Category.StoredCategory(
                "customId",
                LocalesWithText(mapOf("en_US" to "custom")),
                false,
                1
            )
        }
        val viewModel = createViewModel(prefs = prefs, categoriesUseCase = categoriesUseCase)
        viewModel.requestResetEverything()

        viewModel.confirmDialog()

        assertNull(viewModel.uiState.value.dialogTarget)
        assertEquals(SensitivityViewModel.DWELL_TIME_ONE_SECOND, prefs.getDwellTime())
        assertEquals(1, categoriesUseCase._categories.value.size)
    }

    @Test
    fun `confirmDialog for selected only resets checked domains`() = runTest {
        val prefs = FakeVocableSharedPreferences(dwellTime = 3000L, selectedVoiceName = "custom-voice")
        val faceTrackingPermissions = FakeFaceTrackingPermissions(enabled = false)
        val viewModel = createViewModel(prefs = prefs, faceTrackingPermissions = faceTrackingPermissions)
        viewModel.toggleDomain(ResetDomain.SENSITIVITY)
        viewModel.requestResetSelected()

        viewModel.confirmDialog()

        assertNull(viewModel.uiState.value.dialogTarget)
        assertTrue(viewModel.uiState.value.checkedDomains.isEmpty())
        assertEquals(SensitivityViewModel.DWELL_TIME_ONE_SECOND, prefs.getDwellTime())
        // Voice was not checked, so it should be untouched.
        assertEquals("custom-voice", prefs.getSelectedVoiceName())
        // Selection Mode was not checked, so it should be untouched.
        assertFalse(faceTrackingPermissions.resetToDefaultCalled)
    }

    @Test
    fun `confirmDialog for selected mode resets head tracking via permissions`() = runTest {
        val faceTrackingPermissions = FakeFaceTrackingPermissions(enabled = false)
        val viewModel = createViewModel(faceTrackingPermissions = faceTrackingPermissions)
        viewModel.toggleDomain(ResetDomain.SELECTION_MODE)
        viewModel.requestResetSelected()

        viewModel.confirmDialog()

        assertTrue(faceTrackingPermissions.resetToDefaultCalled)
    }

    @Test
    fun `confirmDialog for selected phrases calls the preset-scoped reset, not the nuclear one`() = runTest {
        val phrasesUseCase = FakePhrasesUseCase()
        val viewModel = createViewModel(phrasesUseCase = phrasesUseCase)
        viewModel.toggleDomain(ResetDomain.PHRASES)
        viewModel.requestResetSelected()

        viewModel.confirmDialog()

        // Actual preset-vs-custom scoping is exercised against Room in PhrasesUseCaseTest; this
        // only confirms the PHRASES domain calls the scoped method, not resetToDefaults() (which
        // also wipes genuinely custom phrases and backs the separate "Reset Everything" option).
        assertTrue(phrasesUseCase.resetPresetPhrasesToDefaultsCalled)
        assertFalse(phrasesUseCase.resetToDefaultsCalled)
    }

    @Test
    fun `confirmDialog for everything emits a success toast event`() = runTest {
        val viewModel = createViewModel()
        viewModel.requestResetEverything()

        viewModel.event.test {
            viewModel.confirmDialog()
            assertEquals(ResetSettingsEvent.ShowResetResult(success = true), awaitItem())
        }
    }

    @Test
    fun `confirmDialog for selected emits a success toast event`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleDomain(ResetDomain.SENSITIVITY)
        viewModel.requestResetSelected()

        viewModel.event.test {
            viewModel.confirmDialog()
            assertEquals(ResetSettingsEvent.ShowResetResult(success = true), awaitItem())
        }
    }

    @Test
    fun `confirmDialog emits a failure toast event when a reset call throws`() = runTest {
        val categoriesUseCase = FakeCategoriesUseCase().apply { shouldThrowOnReset = true }
        val viewModel = createViewModel(categoriesUseCase = categoriesUseCase)
        viewModel.requestResetEverything()

        viewModel.event.test {
            viewModel.confirmDialog()
            assertEquals(ResetSettingsEvent.ShowResetResult(success = false), awaitItem())
        }
    }

    @Test
    fun `all domains fit on one page by default`() = runTest {
        val viewModel = createViewModel()

        assertEquals(ResetDomain.entries.size, viewModel.uiState.value.itemsPerPage)
        assertEquals(1, viewModel.uiState.value.totalPages)
    }

    @Test
    fun `updateItemsPerPage below domain count creates multiple pages`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateItemsPerPage(2)

        assertEquals(2, viewModel.uiState.value.itemsPerPage)
        assertEquals(3, viewModel.uiState.value.totalPages)
    }

    @Test
    fun `nextPage and prevPage wrap around totalPages`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateItemsPerPage(2)

        viewModel.nextPage()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.prevPage()
        assertEquals(0, viewModel.uiState.value.currentPage)

        viewModel.prevPage()
        assertEquals(2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `updateItemsPerPage coerces currentPage back into range when it grows`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateItemsPerPage(2)
        viewModel.nextPage()
        viewModel.nextPage()
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.updateItemsPerPage(ResetDomain.entries.size)

        assertEquals(0, viewModel.uiState.value.currentPage)
        assertEquals(1, viewModel.uiState.value.totalPages)
    }
}
