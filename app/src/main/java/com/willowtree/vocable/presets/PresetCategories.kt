package com.willowtree.vocable.presets

import com.willowtree.vocable.R

enum class PresetCategories(val id: String, val initialSortOrder: Int) {
    GENERAL("preset_general_category_id", 0),
    BASIC_NEEDS("preset_basic_needs_category_id", 1),
    CONVERSATION("preset_conversation_category_id", 2),
    ENVIRONMENT("preset_environment_category_id", 3),
    PERSONAL_CARE("preset_personal_care_category_id", 4),
    USER_KEYPAD("preset_user_keypad", 5),
    USER_FAVORITES("preset_user_favorites", 6);

    fun getArrayId(): Int {
        return when (this) {
            GENERAL -> R.array.category_general
            BASIC_NEEDS -> R.array.category_basic_needs
            CONVERSATION -> R.array.category_conversation
            ENVIRONMENT -> R.array.category_environment
            PERSONAL_CARE -> R.array.category_personal_care
            USER_KEYPAD -> -1 // Not localized with same convention
            USER_FAVORITES -> -1 // Not localized with same convention
        }
    }

    fun getNameId(): Int {
        return when (this) {
            GENERAL -> R.string.preset_general
            BASIC_NEEDS -> R.string.preset_basic_needs
            CONVERSATION -> R.string.preset_conversation
            ENVIRONMENT -> R.string.preset_environment
            PERSONAL_CARE -> R.string.preset_personal_care
            USER_KEYPAD -> R.string.preset_user_keypad
            USER_FAVORITES -> R.string.preset_user_favorites
        }
    }
}