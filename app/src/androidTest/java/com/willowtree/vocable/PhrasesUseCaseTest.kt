package com.willowtree.vocable

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.basetest.utils.FakeLocaleProvider
import com.willowtree.vocable.domain.model.CustomPhrase
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.data.repository.RoomStoredPhrasesRepository
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.domain.usecase.PhrasesUseCase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.VocableKoinTestRule
import com.willowtree.vocable.core.UUIDProvider
import com.willowtree.vocable.core.locale.LocalesWithText
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhrasesUseCaseTest {

    @get:Rule
    val vocableKoinTestRule = VocableKoinTestRule()

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
    private val testLocalesWithText = LocalesWithText(
        mapOf("en" to "text")
    )
    private val localeProvider = FakeLocaleProvider()

    private fun createUseCase(): PhrasesUseCase {
        return PhrasesUseCase(
            storedPhrasesRepository = storedPhrasesRepository,
            presetPhrasesRepository = presetPhrasesRepository,
            dateProvider = dateProvider,
            uuidProvider = object : UUIDProvider {
                override fun randomUUIDString(): String = "random"
            },
            localeProvider = localeProvider
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
        presetPhrasesRepository.populateDatabase()
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
            dateProvider.time = (100 + i).toLong()
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

    @Test
    fun addPhrase_createsCustomPhrase() = runTest {
        val useCase = createUseCase()

        useCase.addPhrase(testLocalesWithText, PresetCategories.GENERAL.id)

        assertEquals(
            listOf(
                CustomPhrase(
                    phraseId = "random",
                    sortOrder = 0,
                    localizedUtterance = testLocalesWithText,
                    lastSpokenDate = null,
                )
            ),
            useCase.getPhrasesForCategory(PresetCategories.GENERAL.id)
        )
    }

    private fun getResourceNamesForCategory(categoryName: String): List<String> {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val categoryArrayId = resources.getIdentifier(categoryName, "array", ApplicationProvider.getApplicationContext<Context>().packageName)
        val phrasesIds = resources.obtainTypedArray(categoryArrayId)
        val result = mutableListOf<String>()
        for (index in 0 until phrasesIds.length()) {
            val phraseId = phrasesIds.getResourceId(index, 0)
            result.add(resources.getResourceEntryName(phraseId))
        }
        phrasesIds.recycle()
        return result
    }
}
