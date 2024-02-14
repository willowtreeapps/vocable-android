package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category

class FakeLocalizedResourceUtility : ILocalizedResourceUtility {
    override fun getTextFromCategory(category: Category?): String {
        return when(category) {
            is Category.PresetCategory -> category.categoryId
            is Category.Recents -> "Recents"
            is Category.StoredCategory -> category.localizedName.localesTextMap.entries.first().value
            null -> ""
        }
    }
}