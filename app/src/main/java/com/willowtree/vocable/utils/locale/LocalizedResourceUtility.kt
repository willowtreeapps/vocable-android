package com.willowtree.vocable.utils.locale

import android.content.res.Resources
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import org.koin.core.component.KoinComponent

class LocalizedResourceUtility(
    val resources: Resources
) : KoinComponent, ILocalizedResourceUtility {

    override fun getTextFromCategory(category: Category?): String {
        return category?.localizedName?.localizedText?.text() ?: category?.resourceId?.let {
            if (it != 0) {
                resources.getString(it)
            } else {
                ""
            }
        } ?: ""
    }

    fun getTextFromPhrase(phrase: Phrase?): String {
        return phrase?.utteranceStringRes?.let {
            if (it != 0 && it != -1) {
                resources.getString(it)
            } else {
                null
            }
        } ?: phrase?.localizedUtterance?.localizedText?.text() ?: ""

    }
}