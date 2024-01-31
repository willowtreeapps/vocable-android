package com.willowtree.vocable

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.RoomStoredPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.StubLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utils.locale.LocalesWithText
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PhrasesUseCaseTest {

    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    ).build()

    private val dateProvider = FakeDateProvider()
    private val presetPhrasesRepository = RoomPresetPhrasesRepository(database, dateProvider)
    private val storedPhrasesRepository = RoomStoredPhrasesRepository(database, dateProvider)
    private val legacyRepository = StubLegacyCategoriesAndPhrasesRepository()
    private val testLocalesWithText = LocalesWithText(
        mapOf("en" to "text")
    )

    private fun createUseCase(): PhrasesUseCase {
        return PhrasesUseCase(
            legacyPhrasesRepository = legacyRepository,
            storedPhrasesRepository = storedPhrasesRepository,
            presetPhrasesRepository = presetPhrasesRepository,
            dateProvider = FakeDateProvider()
        )
    }

    @Test
    fun phrase_spoken_updates_stored_and_preset() = runTest {
        val useCase = createUseCase()
        presetPhrasesRepository.populateDatabase()
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = 1L,
                localizedUtterance = testLocalesWithText,
                parentCategoryId = PresetCategories.GENERAL.id,
                creationDate = 0L,
                lastSpokenDate = 100L,
                sortOrder = 0
            )
        )

        dateProvider.time = 123L
        useCase.updatePhraseLastSpokenTime("category_123_0")
        dateProvider.time = 456L
        useCase.updatePhraseLastSpokenTime("1")

        assertEquals(
            123L,
            presetPhrasesRepository.getAllPresetPhrases()
                .first { it.phraseId == "category_123_0" }
                .lastSpokenDate
        )
        assertEquals(
            456L,
            database.phraseDao().getPhrasesForCategory(PresetCategories.GENERAL.id)
                .map { it.asPhrase() }
                .first { it.phraseId == "1" }
                .lastSpokenDate
        )
    }
}