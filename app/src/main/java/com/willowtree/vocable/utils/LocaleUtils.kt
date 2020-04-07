package com.willowtree.vocable.utils

import java.util.*

object LocaleUtils {

    private const val LOCALE_DELIMITER = "_"

    fun getTextForLocale(localizedPairs: Map<String, String>): String {
        return getLocalizedPair(localizedPairs).first
    }

    /**
     * Gets a <String, Locale> Pair that contains the text to be displayed and/or spoken and the
     * closest matching Locale for the text. This method will first try to find the string for the
     * device's default locale. Then it will try to find the string that matches
     * just the language of the device's default locale. It will then try to default to the English
     * language locale. If no English version of the string exists, it will simply return the first
     * string in the map and its corresponding locale.
     *
     * @param localizedPairs The map of language/locale codes and corresponding strings
     * @return A Pair representing the closest matching text and locale
     */
    fun getLocalizedPair(localizedPairs: Map<String, String>): Pair<String, Locale> {
        var locale: Locale? = null
        var text: String? = null
        getLocaleList().forEach {
            if (text != null) return@forEach
            locale = it
            text = localizedPairs[it.toString()]
        }
        if (text == null) {
            localizedPairs.keys.firstOrNull()?.let {
                locale = getLocaleForLanguage(it)
                text = localizedPairs[it]
            }
        }
        return Pair(text ?: "", locale ?: Locale.ENGLISH)
    }

    private fun getLocaleList(): List<Locale> {
        val defaultLocale = Locale.getDefault()
        return mutableListOf<Locale>().apply {
            // First the device's default locale
            add(defaultLocale)
            // Then just the language locale
            add(Locale(defaultLocale.language))
            // Default to English
            add(Locale.ENGLISH)
        }
    }

    private fun getLocaleForLanguage(language: String): Locale {
        val split = language.split(LOCALE_DELIMITER)
        val languageStr = split.first()
        val countryStr = split.getOrNull(1) ?: ""
        return Locale(languageStr, countryStr)
    }
}