package com.willowtree.vocable.ui.presets

import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.PhraseGridItem

/** State for the Presets screen. */
data class PresetsState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val currentPhrases: List<PhraseGridItem> = emptyList(),
    val currentPhrasesCategoryId: String? = null,
    val isSpeaking: Boolean = false,
    val activeText: String = ""
)