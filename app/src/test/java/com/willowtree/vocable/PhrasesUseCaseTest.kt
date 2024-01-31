package com.willowtree.vocable

import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.FakeLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.PresetPhrasesRepository
import com.willowtree.vocable.room.StoredPhrasesRepository
import com.willowtree.vocable.utils.FakeDateProvider
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@Deprecated(
    message = "Migrating to prefer androidTest/*/PhrasesUseCaseTest instead, as it is backed by a" +
            " real room instance.",
    replaceWith = ReplaceWith("androidTest/*/PhrasesUseCaseTest")
)
class PhrasesUseCaseTest {

    private val presetsRepository = FakeLegacyCategoriesAndPhrasesRepository()
    private val testLocalesWithText = LocalesWithText(
        mapOf("en" to "text")
    )

    private fun createUseCase(): PhrasesUseCase {
        return PhrasesUseCase(
            legacyPhrasesRepository = presetsRepository,
            storedPhrasesRepository = StubStoredPhrasesRepository(),
            presetPhrasesRepository = StubPresetPhrasesRepository(),
            dateProvider = FakeDateProvider(),
        )
    }

    @Test
    fun `phrases for recents category pulls recents`() = runTest {
        presetsRepository._recentPhrases = listOf(
            PhraseDto(
                phraseId = 1L,
                parentCategoryId = "1",
                creationDate = 0L,
                lastSpokenDate = 0L,
                localizedUtterance = testLocalesWithText,
                sortOrder = 0
            )
        )
        val useCase = createUseCase()

        assertEquals(
            listOf(
                CustomPhrase(
                    phraseId = "1",
                    localizedUtterance = testLocalesWithText,
                    sortOrder = 0,
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
                    localizedUtterance = testLocalesWithText,
                    sortOrder = 0
                )
            )
        )
        val useCase = createUseCase()

        assertEquals(
            listOf(
                CustomPhrase(
                    phraseId = "1",
                    localizedUtterance = testLocalesWithText,
                    sortOrder = 0,
                )
            ),
            useCase.getPhrasesForCategory("category")
        )
    }

    // Temporary stubs for repositories- when we implement getPhrasesForCategory using the two
    // separate preset/stored repositories, we should use the real impls and move these tests to
    // androidTest/PhrasesUseCaseTest.
    class StubPresetPhrasesRepository : PresetPhrasesRepository {
        override suspend fun populateDatabase() = error("Not implemented")

        override suspend fun getAllPresetPhrases() = error("Not implemented")

        override suspend fun updatePhraseLastSpokenTime(phraseId: String) = error("Not implemented")
    }

    class StubStoredPhrasesRepository : StoredPhrasesRepository {
        override suspend fun addPhrase(phrase: PhraseDto) = error("Not implemented")

        override suspend fun updatePhraseLastSpokenTime(phraseId: String) = error("Not implemented")
    }
}