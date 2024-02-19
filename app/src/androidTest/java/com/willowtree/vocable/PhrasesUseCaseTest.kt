package com.willowtree.vocable

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.RoomStoredPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.StubLegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.utils.UUIDProvider
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
    private val presetPhrasesRepository = RoomPresetPhrasesRepository(
        presetPhrasesDao = database.presetPhrasesDao(),
        dateProvider = dateProvider,
    )
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
            dateProvider = dateProvider,
            uuidProvider = object : UUIDProvider {
                override fun randomUUIDString(): String = "random"
            }
        )
    }

    @Test
    fun getPhrasesForRecentsCategory_returnsRecentPhrases() = runTest {
        val useCase = createUseCase()
        val phraseId = "1"
        presetPhrasesRepository.populateDatabase()
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = phraseId,
                localizedUtterance = testLocalesWithText,
                parentCategoryId = PresetCategories.GENERAL.id,
                creationDate = 0L,
                lastSpokenDate = 100L,
                sortOrder = 0
            )
        )
        dateProvider.time = 101L
        presetPhrasesRepository.updatePhraseLastSpokenTime("category_123_0")

        val recentPhrases = useCase.getPhrasesForCategory(PresetCategories.RECENTS.id)

        assertEquals(2, recentPhrases.size)
        assertEquals(phraseId, recentPhrases[1].phraseId)
        assertEquals("category_123_0", recentPhrases[0].phraseId)
    }

    @Test
    fun getPhrasesForRecentsCategory_limitedToEightPhrases() = runTest {
        val useCase = createUseCase()
        // Add 10 phrases to recents for both stored and preset phrase repos
        for (i in 0..9) {
            storedPhrasesRepository.addPhrase(
                PhraseDto(
                    phraseId = i.toString(),
                    localizedUtterance = testLocalesWithText,
                    parentCategoryId = PresetCategories.GENERAL.id,
                    creationDate = 0L,
                    lastSpokenDate = i.toLong(),
                    sortOrder = 0
                )
            )
            dateProvider.time = 5L
            presetPhrasesRepository.updatePhraseLastSpokenTime("category_123_$i")
        }

        val recentPhrases = useCase.getPhrasesForCategory(PresetCategories.RECENTS.id)
        assertEquals(8, recentPhrases.size)
    }

    @Test
    fun getPhrasesForCategory_getsPresetPhrases() = runTest {
        presetPhrasesRepository.populateDatabase()
        val useCase = createUseCase()

        val entryNames = getResourceNamesForCategory("category_general")
        assertEquals(
            entryNames,
            useCase.getPhrasesForCategory(PresetCategories.GENERAL.id).map {
                it.phraseId
            }
        )
    }

    private fun getResourceNamesForCategory(categoryId: String): List<String> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val res = context.resources
        val arrayId = res.getIdentifier(categoryId, "array", context.packageName)
        val typedArray = res.obtainTypedArray(arrayId)
        val entryNames = mutableListOf<String>()
        for (i in 0 until typedArray.length()) {
            entryNames.add(res.getResourceEntryName(typedArray.getResourceId(i, 0)))
        }
        typedArray.recycle()
        return entryNames.toList()
    }

    @Test
    fun getPhrasesForCategory_getsStoredPhrasesInCategory() = runTest {
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = "1",
                parentCategoryId = "category",
                creationDate = 0L,
                lastSpokenDate = null,
                localizedUtterance = testLocalesWithText,
                sortOrder = 0
            )
        )
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = "2",
                parentCategoryId = "not-category",
                creationDate = 0L,
                lastSpokenDate = null,
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
                    lastSpokenDate = null
                )
            ),
            useCase.getPhrasesForCategory("category")
        )
    }

    @Test
    fun updatePhrase_updatesCustomPhrase() = runTest {
        val useCase = createUseCase()
        val phraseId = "1"
        val localizedUtterance = LocalesWithText(mapOf("en" to "updated text"))
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = phraseId,
                parentCategoryId = "category",
                creationDate = 0L,
                lastSpokenDate = null,
                localizedUtterance = testLocalesWithText,
                sortOrder = 0
            )
        )

        useCase.updatePhrase(phraseId, localizedUtterance)

        val updatedPhrases = useCase.getPhrasesForCategory("category")
        assertEquals(updatedPhrases.size, 1)
        assertEquals(
            localizedUtterance,
            (updatedPhrases[0] as CustomPhrase).localizedUtterance
        )
    }

    @Test
    fun updatePhrase_updatesPresetPhrase() = runTest {
        val useCase = createUseCase()
        presetPhrasesRepository.populateDatabase()
        val phraseId = "category_123_0"
        val localizedUtterance = LocalesWithText(mapOf("en" to "updated text"))

        useCase.updatePhrase(phraseId, localizedUtterance)

        val newCustomPhrase = useCase.getPhrasesForCategory(PresetCategories.USER_KEYPAD.id)
            .singleOrNull { it.phraseId == phraseId } as CustomPhrase
        assertEquals(localizedUtterance, newCustomPhrase.localizedUtterance)
    }

    @Test
    fun deletePhrase_deletesFromStoredRepository() = runTest {
        val useCase = createUseCase()
        val customPhraseId = "1"
        storedPhrasesRepository.addPhrase(
            PhraseDto(
                phraseId = customPhraseId,
                localizedUtterance = testLocalesWithText,
                parentCategoryId = PresetCategories.GENERAL.id,
                creationDate = 0L,
                lastSpokenDate = 100L,
                sortOrder = 0
            )
        )

        useCase.deletePhrase(customPhraseId)

        val storedPhrase = useCase.getPhrasesForCategory(PresetCategories.GENERAL.id)
            .firstOrNull { it.phraseId == customPhraseId }
        assertEquals(null, storedPhrase)
    }

    @Test
    fun deletePhrase_deletesFromPresetRepository() = runTest {
        val useCase = createUseCase()
        val presetPhraseId = "category_123_0"
        presetPhrasesRepository.populateDatabase()

        useCase.deletePhrase(presetPhraseId)

        val presetPhrase = useCase.getPhrasesForCategory(PresetCategories.USER_KEYPAD.id)
            .firstOrNull { it.phraseId == presetPhraseId }
        assertEquals(null, presetPhrase)
    }
}