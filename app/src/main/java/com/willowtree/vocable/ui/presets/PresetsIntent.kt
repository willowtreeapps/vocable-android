package com.willowtree.vocable.ui.presets

/** Events that the Presets screen can send to its parent. */
sealed interface PresetsIntent {
    data class OnCategorySelected(val categoryId: String) : PresetsIntent
    data class AddToRecents(val phraseId: String) : PresetsIntent
    data class UpdateActiveText(val text: String) : PresetsIntent
    object NavToAddPhrase : PresetsIntent
}