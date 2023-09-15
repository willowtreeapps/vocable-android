package com.willowtree.vocable.utils

import android.content.Context
import android.content.res.Resources
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.Phrase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LocalizedResourceUtility : KoinComponent, ILocalizedResourceUtility {

    val resources: Resources = get<Context>().resources

    override fun getTextFromCategory(category: Category?): String {

        return category?.localizedName?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: category?.resourceId?.let {
            if (it != 0) {
                resources.getString(it)
            } else {
                ""
            }
        } ?: ""
    }

    fun getTextFromPhrase(phrase: Phrase?): String {
        return phrase?.localizedUtterance?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: ""
    }
}
