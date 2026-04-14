package com.willowtree.vocable.ui.languageselection

sealed interface LanguageSelectionEvent {
    data object NavigateBack : LanguageSelectionEvent
}
