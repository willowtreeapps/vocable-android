package com.willowtree.vocable.utils

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase

interface ILocalizedResourceUtility {
    fun getTextFromCategory(category: Category?): String
    fun getTextFromPhrase(phrase: Phrase?): String
}