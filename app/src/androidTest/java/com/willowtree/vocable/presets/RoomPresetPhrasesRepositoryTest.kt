package com.willowtree.vocable.presets

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.room.VocableDatabase
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPresetPhrasesRepositoryTest {
    private fun createRepository(): RoomPresetPhrasesRepository {
        return RoomPresetPhrasesRepository(
            database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                VocableDatabase::class.java
            ).build()
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
                        )
                    )
                }
                phrasesIds.recycle()
                expectedPhrases
            }
    }
}
