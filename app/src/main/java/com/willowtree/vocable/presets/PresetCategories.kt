package com.willowtree.vocable.presets

import com.willowtree.vocable.R

enum class PresetCategories(val id: String, val initialSortOrder: Int) {
    GENERAL("preset_general", 0),
    BASIC_NEEDS("preset_basic_needs", 1),
    PERSONAL_CARE("preset_personal_care", 2),
    CONVERSATION("preset_conversation", 3),
    ENVIRONMENT("preset_environment", 4 ),
    USER_KEYPAD("preset_user_keypad", 5),
    RECENTS("preset_recents", 6),
    @Deprecated("This is being filtered out from the UI already. Remove this.")
    MY_SAYINGS("preset_user_favorites", 7);

    fun getArrayId(): Int {
        return when (this) {
            GENERAL -> R.array.category_general
            BASIC_NEEDS -> R.array.category_basic_needs
            CONVERSATION -> R.array.category_conversation
            ENVIRONMENT -> R.array.category_environment
            PERSONAL_CARE -> R.array.category_personal_care
            USER_KEYPAD -> R.array.category_123
            RECENTS -> -1
            MY_SAYINGS -> -1 // Not localized with same convention
        }
    }
}