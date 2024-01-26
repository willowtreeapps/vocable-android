package com.willowtree.vocable.room

import android.content.Context
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetPhrase
import com.willowtree.vocable.presets.asPhrase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext.get

class RoomPresetPhrasesRepository(
    private val database: VocableDatabase,
) : PresetPhrasesRepository {

    private val phrasesMutex = Mutex()

    override suspend fun populateDatabase() {
        ensurePopulated()
    }

    override suspend fun getAllPresetPhrases(): List<PresetPhrase> {
        return database.presetPhrasesDao().getAllPresetPhrases()
            .map(PresetPhraseDto::asPhrase)
    }

    private suspend fun ensurePopulated() {
        phrasesMutex.withLock {
            val existingPresetPhrases = database.presetPhrasesDao().getAllPresetPhrases()
            val existingResourceIdStrings = existingPresetPhrases.map { it.phraseId }.toSet()

            PresetCategories.values().forEach { presetCategory ->
                if (presetCategory != PresetCategories.RECENTS && presetCategory != PresetCategories.MY_SAYINGS) {
                    val phrasesIds = get().get<Context>()
                        .resources.obtainTypedArray(presetCategory.getArrayId())
                    val phraseObjects = mutableListOf<PresetPhraseDto>()
                    for (index in 0 until phrasesIds.length()) {
                        val phraseId = phrasesIds.getResourceId(index, 0)
                        val phraseEntryName =
                            get().get<Context>().resources.getResourceEntryName(phraseId)
                        if (phraseEntryName !in existingResourceIdStrings) {
                            phraseObjects.add(
                                PresetPhraseDto(
                                    phraseId = phraseEntryName,
                                    parentCategoryId = presetCategory.id,
                                    creationDate = System.currentTimeMillis(),
                                    lastSpokenDate = null,
                                    sortOrder = phraseObjects.size,
                                )
                            )
                        }
                    }
                    phrasesIds.recycle()
                    addPhrases(phraseObjects)
                }
            }
        }
    }

    private suspend fun addPhrases(presetPhrases: List<PresetPhraseDto>) {
        database.presetPhrasesDao().insertPhrases(presetPhrases)
    }
}