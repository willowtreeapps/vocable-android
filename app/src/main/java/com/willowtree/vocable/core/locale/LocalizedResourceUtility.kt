package com.willowtree.vocable.core.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.Phrase

class LocalizedResourceUtility(
    private val context: Context,
) : ILocalizedResourceUtility {

    private fun localizedContext(): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return context
        val locale = locales[0] ?: return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun getTextFromCategory(category: Category?): String =
        category?.text(localizedContext()) ?: ""

    override fun getTextFromPhrase(phrase: Phrase?): String =
        phrase?.text(localizedContext()) ?: ""
}