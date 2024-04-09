package com.willowtree.vocable.utils.locale

import android.content.Context
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.ILocalizedResourceUtility

class LocalizedResourceUtility(
    private val context: Context,
) : ILocalizedResourceUtility {

    override fun getTextFromCategory(category: Category?): String {
        return category?.text(context) ?: ""
    }

    fun getTextFromPhrase(phrase: Phrase?): String {
        return phrase?.text(context) ?: ""
    }
}