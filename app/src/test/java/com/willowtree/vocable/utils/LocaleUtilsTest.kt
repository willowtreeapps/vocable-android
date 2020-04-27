package com.willowtree.vocable.utils

import org.junit.Assert
import org.junit.Test
import java.util.*

class LocaleUtilsTest {

    companion object {
        private const val EN_HELLO = "Hello"
        private const val FR_CA_HELLO = "Salut"
        private const val FR_HELLO = "Bonjour"
        private const val DE_DE_HELLO = "Hallo"

        private val EN_PAIR = Pair(Locale.ENGLISH.toString(), EN_HELLO)
        private val FR_CA_PAIR = Pair(Locale.CANADA_FRENCH.toString(), FR_CA_HELLO)
        private val FR_PAIR = Pair(Locale.FRENCH.toString(), FR_HELLO)
        private val DE_DE_PAIR = Pair(Locale.GERMANY.toString(), DE_DE_HELLO)
    }

    @Test
    fun `full locale pair returned for full locale`() {
        val localizedPairs = mapOf(FR_CA_PAIR, FR_PAIR, EN_PAIR)
        Locale.setDefault(Locale.CANADA_FRENCH)
        val localizedPair = LocaleUtils.getLocalizedPair(localizedPairs)
        Assert.assertEquals(FR_CA_HELLO, localizedPair.first)
        Assert.assertEquals(Locale.CANADA_FRENCH, localizedPair.second)
    }

    @Test
    fun `language-only locale pair returned for full locale`() {
        val localizedPairs = mapOf(FR_PAIR, EN_PAIR)
        Locale.setDefault(Locale.CANADA_FRENCH)
        val localizedPair = LocaleUtils.getLocalizedPair(localizedPairs)
        Assert.assertEquals(FR_HELLO, localizedPair.first)
        Assert.assertEquals(Locale.FRENCH, localizedPair.second)
    }

    @Test
    fun `English locale pair returned for full locale`() {
        val localizedPairs = mapOf(EN_PAIR)
        Locale.setDefault(Locale.CANADA_FRENCH)
        val localizedPair = LocaleUtils.getLocalizedPair(localizedPairs)
        Assert.assertEquals(EN_HELLO, localizedPair.first)
        Assert.assertEquals(Locale.ENGLISH, localizedPair.second)
    }

    @Test
    fun `default to first map value if no value exists for locale`() {
        val localizedPairs = mapOf(DE_DE_PAIR)
        Locale.setDefault(Locale.CANADA_FRENCH)
        val localizedPair = LocaleUtils.getLocalizedPair(localizedPairs)
        Assert.assertEquals(DE_DE_HELLO, localizedPair.first)
        Assert.assertEquals(Locale.GERMANY, localizedPair.second)
    }

    @Test
    fun `empty map returns default values`() {
        val localizedPairs = mapOf<String, String>()
        val localizedPair = LocaleUtils.getLocalizedPair(localizedPairs)
        Assert.assertEquals("", localizedPair.first)
        Assert.assertEquals(Locale.ENGLISH, localizedPair.second)
    }

    @Test
    fun `getTextForLocale returns closest match`() {
        val localizedPairs = mapOf(FR_CA_PAIR, FR_PAIR, EN_PAIR)
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textForLocale = LocaleUtils.getTextForLocale(localizedPairs)
        Assert.assertEquals(FR_CA_HELLO, textForLocale)
    }
}