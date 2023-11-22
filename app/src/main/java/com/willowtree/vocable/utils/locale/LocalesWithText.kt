package com.willowtree.vocable.utils.locale

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Locale

private const val LOCALE_DELIMITER = "_"

/**
 * A typealias for a String containing LOCALE_DELIMITER separated language and country codes
 */
typealias LocaleString = String

private fun LocaleString.toLocale(): Locale {
    val split = split(LOCALE_DELIMITER)
    val languageStr = split.first()
    val countryStr = split.getOrNull(1) ?: ""
    return Locale(languageStr, countryStr)
}

/**
 * A typealias for a map of language codes and corresponding text as Strings
 */
@Parcelize
data class LocalesWithText(
    val localesTextMap: Map<LocaleString, String>
) : Parcelable {

    /**
     * Gets the string corresponding to the given localeString.
     * @param localeString The localeString to match against
     * @return The string corresponding to the given localeString.
     */
    operator fun get(localeString: LocaleString): String? = localesTextMap[localeString]

    /**
     * Gets the string corresponding to the given locale matched against LocaleString.
     * @param locale The locale to match against turned into String
     */
    private operator fun get(locale: Locale): String? = localesTextMap[locale.toString()]

    /**
     * Sets the string corresponding to the given localeString.
     * @param localeString String version of the locale
     * @param text for the localeString
     * @return A new LocalesWithText with the given localeString and text added to the map
     */
    operator fun set(localeString: LocaleString, text: String): LocalesWithText {
        return LocalesWithText(localesTextMap.toMutableMap().apply {
            this[localeString] = text
        })
    }

    /**
     * Checks if the contains the given string.
     */
    fun containsValue(phraseStr: String): Boolean = localesTextMap.containsValue(phraseStr)

    /**
     * Gets a list of all the languages in the map
     * @return A list of all the languages in the map, keys.toList()
     */
    @IgnoredOnParcel
    val locales: List<LocaleString> = localesTextMap.keys.toList()

    /**
     * Gets a TextWithLocale that contains the text to be displayed and/or spoken and the
     * closest matching Locale for the text. This method will first try to find the string for the
     * device's default locale. Then it will try to find the string that matches
     * just the language of the device's default locale. It will then try to default to the English
     * language locale. If no English version of the string exists, it will simply return the first
     * string in the map and its corresponding locale.
     *
     * @return A text for localization
     */
    @IgnoredOnParcel
    val localizedText: LocaleWithText
        get() {
            var locale: Locale? = null
            var text: String? = null
            // Attempts to localize text given a list of default Locales
            defaultLocaleList.forEach {
                if (text != null) return@forEach
                locale = it
                text = this[it]
            }
            if (text == null) {
                locales.firstOrNull()?.let {
                    locale = it.toLocale()
                    text = this[it]
                }
            }
            return Pair(locale ?: Locale.ENGLISH, text ?: "")
        }

    @IgnoredOnParcel
    private val defaultLocaleList: List<Locale>
        get() {
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

}