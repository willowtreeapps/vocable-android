package com.willowtree.vocable.presets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.domain.model.PresetPhrase
import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.data.room.PresetPhraseDto
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

    private fun createDatabase(): VocableDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        VocableDatabase::class.java
    ).build()

    private fun createRepository(
        database: VocableDatabase = createDatabase()
    ): RoomPresetPhrasesRepository {
        return RoomPresetPhrasesRepository(
            presetPhrasesDao = database.presetPhrasesDao(),
            phraseDao = database.phraseDao(),
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
        val database = createDatabase()
        val dao = database.presetPhrasesDao()
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

        createRepository(database).populateDatabase()

        val keypadPhrase = dao.getPhrase("category_123_0")!!
        assertEquals(9, keypadPhrase.sortOrder)
        // The resync must not clobber unrelated columns.
        assertEquals(1234L, keypadPhrase.lastSpokenDate)
        assertEquals(false, keypadPhrase.deleted)
    }

    @Test
    fun given_a_stale_phrase_populateDatabase_does_not_duplicate_or_misorder_the_category() = runTest {
        val database = createDatabase()
        database.presetPhrasesDao().insertPhrases(
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

        val repository = createRepository(database)
        repository.populateDatabase()

        // Newly inserted phrases take their array index too, so they can't collide with the
        // sort order of a phrase that was already present.
        assertEquals(
            makePresetPhrases().filter { it.parentCategoryId == PresetCategories.USER_KEYPAD.id },
            repository.getPhrasesForCategory(PresetCategories.USER_KEYPAD.id)
                .sortedBy { it.sortOrder }
        )
    }

    @Test
    fun given_an_edited_preset_populateDatabase_resyncs_the_stored_shadow_phrase() = runTest {
        val database = createDatabase()
        // What an install that edited the "0" key before the reorder looks like: the preset row is
        // soft-deleted and a stored phrase shadows it, both carrying the pre-reorder index. The
        // shadow has to be renumbered alongside the preset, or the two disagree and the category
        // renders with a duplicated sort order - and one phrase too many for its page.
        database.presetPhrasesDao().insertPhrases(
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
        database.presetPhrasesDao().deletePhrase("category_123_0", deleted = true)
        database.phraseDao().insertPhrase(
            PhraseDto(
                phraseId = "category_123_0",
                parentCategoryId = PresetCategories.USER_KEYPAD.id,
                creationDate = 0L,
                lastSpokenDate = 1234L,
                localizedUtterance = LocalesWithText(mapOf("en" to "zero")),
                sortOrder = 0,
            )
        )

        createRepository(database).populateDatabase()

        val shadow = database.phraseDao().getPhrase("category_123_0")!!
        assertEquals(9, shadow.sortOrder)
        // The resync must not clobber the user's edit.
        assertEquals(LocalesWithText(mapOf("en" to "zero")), shadow.localizedUtterance)
        assertEquals(1234L, shadow.lastSpokenDate)
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
