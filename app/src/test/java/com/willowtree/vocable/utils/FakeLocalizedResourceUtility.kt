package com.willowtree.vocable.utils

import com.willowtree.vocable.room.CategoryDto

class FakeLocalizedResourceUtility : ILocalizedResourceUtility {
    override fun getTextFromCategory(category: CategoryDto?): String {
        return category?.localizedName?.entries?.first()?.value ?: ""
    }
}