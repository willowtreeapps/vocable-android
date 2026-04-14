package com.willowtree.vocable.ui.languageselection

data class LanguageSelectionState(
    val languages: List<LanguageOption> = emptyList(),
    val selectedLanguageTag: String? = null
)

data class LanguageOption(
    val tag: String,
    val displayName: String
)
