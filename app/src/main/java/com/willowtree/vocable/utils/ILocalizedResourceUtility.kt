package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category

interface ILocalizedResourceUtility {
    fun getTextFromCategory(category: Category?): String
}