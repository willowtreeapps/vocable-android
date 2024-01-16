package com.willowtree.vocable.utils.locale

import android.content.res.Resources
import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.PresetPhrase
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.every
import org.mockito.ArgumentMatchers.any


class LocalizedResourceUtilityTest {

    private lateinit var localizedResourceUtility: LocalizedResourceUtility
    private lateinit var resources: Resources

    @Before
    fun setup() {
        resources = mockk()
        every { resources.getString(any()) } returns "Text from resources"
        localizedResourceUtility = LocalizedResourceUtility(resources)
    }

    @Test
    fun `getTextFromPhrase returns correct text when utteranceStringRes is set`() {
        val phrase = PresetPhrase(
            phraseId = 0,
            sortOrder = 0,
            utteranceStringRes = 1,
        )

        val result = localizedResourceUtility.getTextFromPhrase(phrase)

        assertEquals("Text from resources", result)
    }

    @Test
    fun `getTextFromPhrase returns correct text when localizedUtterance is set`() {
        val phrase = CustomPhrase(
            phraseId = 0,
            sortOrder = 0,
            localizedUtterance = LocalesWithText(
                mapOf(
                    "en" to "Localized phrase"
                )
            )
        )

        val result = localizedResourceUtility.getTextFromPhrase(phrase)

        assertEquals("Localized phrase", result)
    }

}