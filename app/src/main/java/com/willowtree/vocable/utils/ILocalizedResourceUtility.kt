package com.willowtree.vocable.utils

import com.willowtree.vocable.room.CategoryDto

interface ILocalizedResourceUtility {
    fun getTextFromCategory(category: CategoryDto?): String
}