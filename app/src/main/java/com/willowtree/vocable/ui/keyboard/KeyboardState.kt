package com.willowtree.vocable.ui.keyboard

/** State for the Keyboard screen. */
data class KeyboardState(
    val inputText: String = "",
    val headTrackingEnabled: Boolean = true,
    val categoryIdToEdit: String? = null,
    val isCategoryEdit: Boolean = false,
    val phraseIdToEdit: String? = null,
    val saveCategoryId: String? = null
)