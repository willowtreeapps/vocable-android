package com.willowtree.vocable.core.locale

import android.content.Context
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.Phrase

class LocalizedResourceUtility(
    private val context: Context,
) : ILocalizedResourceUtility {

    override fun getTextFromCategory(category: Category?): String {
        return category?.text(context) ?: ""
    }

    override fun getTextFromPhrase(phrase: Phrase?): String {
        return phrase?.text(context) ?: ""
    }
}