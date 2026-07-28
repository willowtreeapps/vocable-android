package com.willowtree.vocable.presets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.domain.model.PresetPhrase
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.data.room.PresetPhraseDto
import com.willowtree.vocable.data.room.PresetPhrasesDao
import com.willowtree.vocable.data.room.VocableDatabase
import com.willowtree.vocable.utility.FakeDateProvider
import com.willowtree.vocable.utility.VocableKoinTestRule
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPresetPhrasesRepositoryTest {

    @get:Rule
    val vocableKoinTestRule = VocableKoinTestRule()

    private fun createDao(): PresetPhrasesDao = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    ).build().presetPhrasesDao()

    private fun createRepository(
        dao: PresetPhrasesDao = createDao()
    ): RoomPresetPhrasesRepository {
        return RoomPresetPhrasesRepository(
            presetPhrasesDao = dao,
            dateProvider = FakeDateProvider()
        )
    }

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun preset_phrases_populated() = runTest {
        val repository = createRepository()

        repository.populateDatabase()

        val expectedPhrases = makePresetPhrases()
        assertEquals(
            expectedPhrases,
            repository.getAllPresetPhrases()
        )
    }

    @Test
    fun given_populateDatabase_called_twice_no_duplicate_phrases_are_added()= runTest {
        val repository = createRepository()

        repository.populateDatabase()
        repository.populateDatabase()

        val expectedPhrases = makePresetPhrases()
        assertEquals(
            expectedPhrases,
            repository.getAllPresetPhrases()
        )
    }

    @Test
    fun given_a_phrase_seeded_with_a_stale_sort_order_populateDatabase_resyncs_it() = runTest {
        val dao = createDao()
        // A phrase seeded by an earlier release, before R.array.category_123 was reordered into
        // phone-keypad order. Without a resync the array is only the source of truth on a fresh
        // install and the reorder is invisible to anyone who already ran the app - see #611.
        dao.insertPhrases(
            listOf(
                PresetPhraseDto(
                    phraseId = "category_123_0",
                    parentCategoryId = PresetCategories.USER_KEYPAD.id,
                    creationDate = 0L,
                    lastSpokenDate = 1234L,
                    sortOrder = 0,
                )
            )
        )

        createRepository(dao).populateDatabase()

        val keypadPhrase = dao.getPhrase("category_123_0")!!
        assertEquals(9, keypadPhrase.sortOrder)
        // The resync must not clobber unrelated columns.
        assertEquals(1234L, keypadPhrase.lastSpokenDate)
        assertEquals(false, keypadPhrase.deleted)
    }

    @Test
    fun given_a_stale_phrase_populateDatabase_does_not_duplicate_or_misorder_the_category() = runTest {
        val dao = createDao()
        dao.insertPhrases(
            listOf(
                PresetPhraseDto(
                    phraseId = "category_123_0",
                    parentCategoryId = PresetCategories.USER_KEYPAD.id,
                    creationDate = 0L,
                    lastSpokenDate = null,
                    sortOrder = 0,
                )
            )
        )

        val repository = createRepository(dao)
        repository.populateDatabase()

        // Newly inserted phrases take their array index too, so they can't collide with the
        // sort order of a phrase that was already present.
        assertEquals(
            makePresetPhrases().filter { it.parentCategoryId == PresetCategories.USER_KEYPAD.id },
            repository.getPhrasesForCategory(PresetCategories.USER_KEYPAD.id)
                .sortedBy { it.sortOrder }
        )
    }

    private fun makePresetPhrases(): List<PresetPhrase> {
        return PresetCategories.values()
            .filterNot { it == PresetCategories.MY_SAYINGS || it == PresetCategories.RECENTS }
            .flatMap { presetCategory ->
                val phrasesIds = resources.obtainTypedArray(presetCategory.getArrayId())
                val expectedPhrases = mutableListOf<PresetPhrase>()
                for (index in 0 until phrasesIds.length()) {
                    val phraseId = phrasesIds.getResourceId(index, 0)
                    val phraseEntryName = resources.getResourceEntryName(phraseId)
                    expectedPhrases.add(
                        PresetPhrase(
                            phraseId = phraseEntryName,
                            sortOrder = index,
                            lastSpokenDate = null,
                            parentCategoryId = presetCategory.id,
                            deleted = false,
                        )
                    )
                }
                phrasesIds.recycle()
                expectedPhrases
            }
    }
}
