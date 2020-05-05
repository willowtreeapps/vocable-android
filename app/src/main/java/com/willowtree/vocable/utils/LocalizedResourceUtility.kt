package com.willowtree.vocable.utils

import android.content.Context
import android.content.res.Resources
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import org.koin.core.KoinComponent
import org.koin.core.get
import java.util.*

class LocalizedResourceUtility(context: Context) : KoinComponent {

    val resources: Resources = get<Context>().resources

    fun getValueLocalePairFromPhrase(phrase: Phrase): Pair<String, Locale> {
        return phrase.resourceId?.let {
            Pair(resources.getString(it), Locale.getDefault())
        } ?: phrase.localizedUtterance?.let {
            LocaleUtils.getLocalizedPair(it)
        } ?: Pair("", Locale.getDefault())
    }

    fun getTextFromCategory(category: Category): String {
        return category.resourceId?.let {
            resources.getString(it)
        } ?: category.localizedName?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: ""
    }

    fun getTextFromPhrase(phrase: Phrase): String {
        return phrase.resourceId?.let {
            resources.getString(it)
        } ?: phrase.localizedUtterance?.let {
            LocaleUtils.getTextForLocale(it)
        } ?: ""
    }
}