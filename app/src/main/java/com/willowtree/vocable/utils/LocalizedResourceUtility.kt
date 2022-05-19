package com.willowtree.vocable.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class LocalizedResourceUtility : KoinComponent {

    val resources: Resources = get<Context>().resources

    fun getTextFromCategory(category: Category?): String {

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