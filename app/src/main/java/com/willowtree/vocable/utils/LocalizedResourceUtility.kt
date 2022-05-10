package com.willowtree.vocable.utils

import android.content.Context
import android.content.res.Resources
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LocalizedResourceUtility : KoinComponent {

    val resources: Resources = get<Context>().resources

    fun getTextFromCategory(category: Category?): String {
        if (category?.resourceId == 0) {
            return category.localizedName?.let {
                LocaleUtils.getTextForLocale(it)
            } ?: ""
        }
        return category?.resourceId?.let {
            resources.getString(it)
        } ?: category?.localizedName?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: ""
    }

    fun getTextFromPhrase(phrase: Phrase?): String {
        return phrase?.localizedUtterance?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: ""
    }
}