package com.willowtree.vocable

import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.FakeLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.FakeDateProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PhrasesUseCaseTest {

    private val presetsRepository = FakeLegacyCategoriesAndPhrasesRepository()

    private fun createUseCase(): PhrasesUseCase {
        return PhrasesUseCase(presetsRepository, FakeDateProvider())
    }

    @Test
    fun `phrases for recents category pulls recents`() = runTest {
        presetsRepository._recentPhrases = listOf(
            PhraseDto(
                phraseId = 1L,
                parentCategoryId = "1",
                creationDate = 0L,
                lastSpokenDate = 0L,
                localizedUtterance = null,
                sortOrder = 0
            )
        )
        val useCase = createUseCase()

        assertEquals(
            listOf(
                CustomPhrase(
                    phraseId = 1L,
                    localizedUtterance = null,
                    sortOrder = 0
                )
            ),
            useCase.getPhrasesForCategory(PresetCategories.RECENTS.id)
        )
    }

    @Test
    fun `phrases for stored category pulls stored`() = runTest {
        presetsRepository._categoriesToPhrases = mapOf(
            "category" to listOf(
                PhraseDto(
                    phraseId = 1L,
                    parentCategoryId = "1",
                    creationDate = 0L,
                    lastSpokenDate = 0L,
                    localizedUtterance = null,
                    sortOrder = 0
                )
            )
        )
        val useCase = createUseCase()

        assertEquals(
            listOf(
                CustomPhrase(
                    phraseId = 1L,
                    localizedUtterance = null,
                    sortOrder = 0
                )
            ),
            useCase.getPhrasesForCategory("category")
        )
    }

}