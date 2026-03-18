package com.willowtree.vocable.core

import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.Phrase

interface ILocalizedResourceUtility {
    fun getTextFromCategory(category: Category?): String
    fun getTextFromPhrase(phrase: Phrase?): String
}