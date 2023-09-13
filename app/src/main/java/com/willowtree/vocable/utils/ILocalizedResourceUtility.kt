package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategoryDto

interface ILocalizedResourceUtility {
    fun getTextFromCategory(category: Category?): String
    fun getTextFromCategory(category: CategoryDto?): String
}