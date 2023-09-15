package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category

class FakeLocalizedResourceUtility : ILocalizedResourceUtility {
    override fun getTextFromCategory(category: Category?): String {
        return category?.localizedName?.entries?.first()?.value ?: ""
    }
}