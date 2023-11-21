package com.willowtree.vocable.utils

import com.willowtree.vocable.utils.locale.LocalesWithText
import com.willowtree.vocable.utils.locale.locale
import com.willowtree.vocable.utils.locale.text
import org.junit.Assert
import org.junit.Test
import java.util.Locale

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
        val localizedPairs = LocalesWithText(mapOf(FR_CA_PAIR, FR_PAIR, EN_PAIR))
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textWithLocal = localizedPairs.localizedText
        Assert.assertEquals(FR_CA_HELLO, textWithLocal.text())
        Assert.assertEquals(Locale.CANADA_FRENCH, textWithLocal.locale())
    }

    @Test
    fun `language-only locale pair returned for full locale`() {
        val localizedPairs = LocalesWithText(mapOf(FR_PAIR, EN_PAIR))
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textWithLocal = localizedPairs.localizedText
        Assert.assertEquals(FR_HELLO, textWithLocal.text())
        Assert.assertEquals(Locale.FRENCH, textWithLocal.locale())
    }

    @Test
    fun `English locale pair returned for full locale`() {
        val localizedPairs = LocalesWithText(mapOf(EN_PAIR))
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textWithLocal = localizedPairs.localizedText
        Assert.assertEquals(EN_HELLO, textWithLocal.text())
        Assert.assertEquals(Locale.ENGLISH, textWithLocal.locale())
    }

    @Test
    fun `default to first map value if no value exists for locale`() {
        val localizedPairs = LocalesWithText(mapOf(DE_DE_PAIR))
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textWithLocal = localizedPairs.localizedText
        Assert.assertEquals(DE_DE_HELLO, textWithLocal.text())
        Assert.assertEquals(Locale.GERMANY, textWithLocal.locale())
    }

    @Test
    fun `empty map returns default values`() {
        val localizedPairs = LocalesWithText(mapOf())
        val textWithLocal = localizedPairs.localizedText
        Assert.assertEquals("", textWithLocal.text())
        Assert.assertEquals(Locale.ENGLISH, textWithLocal.locale())
    }

    @Test
    fun `getTextForLocale returns closest match`() {
        val localizedPairs = LocalesWithText(mapOf(FR_CA_PAIR, FR_PAIR, EN_PAIR))
        Locale.setDefault(Locale.CANADA_FRENCH)
        val textForLocale = localizedPairs.localizedText
        Assert.assertEquals(FR_CA_HELLO, textForLocale.text())
    }
}