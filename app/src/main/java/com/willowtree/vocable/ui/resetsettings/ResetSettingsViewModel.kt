package com.willowtree.vocable.ui.resetsettings

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * ViewModel for the Reset App Settings screen: per-domain checkboxes for granular reset, plus a
 * nuclear "reset everything" option identical in scope to the previous Settings-screen dialog.
 */
class ResetSettingsViewModel(
    private val prefs: IVocableSharedPreferences,
    private val categoriesUseCase: ICategoriesUseCase,
    private val phrasesUseCase: IPhrasesUseCase,
    private val faceTrackingPermissions: IFaceTrackingPermissions
) : BaseViewModel<ResetSettingsState, ResetSettingsEvent>(ResetSettingsState()) {

    fun onBack() {
        sendEvent(ResetSettingsEvent.NavigateBack)
    }

    fun toggleDomain(domain: ResetDomain) {
        updateState {
            copy(checkedDomains = if (domain in checkedDomains) checkedDomains - domain else checkedDomains + domain)
        }
    }

    fun requestResetSelected() {
        if (uiState.value.checkedDomains.isNotEmpty()) {
            updateState { copy(dialogTarget = ResetDialogTarget.Selected) }
        }
    }

    fun requestResetEverything() {
        updateState { copy(dialogTarget = ResetDialogTarget.Everything) }
    }

    fun dismissDialog() {
        updateState { copy(dialogTarget = null) }
    }

    fun nextPage() {
        updateState { copy(currentPage = (currentPage + 1) % totalPages) }
    }

    fun prevPage() {
        updateState { copy(currentPage = if (currentPage - 1 < 0) totalPages - 1 else currentPage - 1) }
    }

    fun updateItemsPerPage(itemsPerPage: Int) {
        updateState {
            val newItemsPerPage = itemsPerPage.coerceAtLeast(1)
            val newTotalPages = ceil(ResetDomain.entries.size.toFloat() / newItemsPerPage).toInt().coerceAtLeast(1)
            copy(
                itemsPerPage = newItemsPerPage,
                currentPage = currentPage.coerceAtMost(newTotalPages - 1)
            )
        }
    }

    fun confirmDialog() {
        val target = uiState.value.dialogTarget
        val domains = uiState.value.checkedDomains
        updateState { copy(dialogTarget = null) }
        viewModelScope.launch {
            try {
                when (target) {
                    ResetDialogTarget.Everything -> {
                        categoriesUseCase.resetToDefaults()
                        prefs.clearAll()
                    }
                    ResetDialogTarget.Selected -> {
                        domains.forEach { domain ->
                            when (domain) {
                                ResetDomain.VOICE -> prefs.setSelectedVoiceName(null)
                                ResetDomain.SENSITIVITY -> prefs.resetSensitivity()
                                ResetDomain.SELECTION_MODE -> faceTrackingPermissions.resetToDefault()
                                ResetDomain.CATEGORIES -> categoriesUseCase.resetCategoriesToDefaults()
                                ResetDomain.PHRASES -> phrasesUseCase.resetPresetPhrasesToDefaults()
                            }
                        }
                        updateState { copy(checkedDomains = emptySet()) }
                    }
                    null -> return@launch
                }
                sendEvent(ResetSettingsEvent.ShowResetResult(success = true))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sendEvent(ResetSettingsEvent.ShowResetResult(success = false))
            }
        }
    }
}
