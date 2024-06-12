package com.willowtree.vocable.room

import android.content.Context
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetPhrase
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.utils.DateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext.get

class RoomPresetPhrasesRepository(
    private val presetPhrasesDao: PresetPhrasesDao,
    private val dateProvider: DateProvider,
) : PresetPhrasesRepository {

    private val phrasesMutex = Mutex()

    override suspend fun populateDatabase() {
        ensurePopulated()
    }

    override suspend fun getAllPresetPhrases(): List<PresetPhrase> {
        return presetPhrasesDao.getAllPresetPhrases()
            .map(PresetPhraseDto::asPhrase)
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        presetPhrasesDao.updatePhraseSpokenDate(
            PhraseSpokenDate(
                phraseId = phraseId,
                lastSpokenDate = dateProvider.currentTimeMillis()
            )
        )
    }

    override suspend fun getRecentPhrases(): List<PresetPhrase> {
        return presetPhrasesDao.getRecentPhrases()
            .filterDeletedPresets()
            .map { it.asPhrase() }
    }

    override fun getRecentPhrasesFlow(): Flow<List<PresetPhrase>> {
        return presetPhrasesDao.getRecentPhrasesFlow()
            .map { phraseList -> phraseList.filterDeletedPresets().map { it.asPhrase() } }
    }

    override suspend fun getPhrasesForCategory(categoryId: String): List<PresetPhrase> {
        return presetPhrasesDao.getPhrasesForCategory(categoryId)
            .filterDeletedPresets()
            .map { it.asPhrase() }
    }

    override fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<PresetPhrase>> {
        return presetPhrasesDao.getPhrasesForCategoryFlow(categoryId)
            .map { phraseList -> phraseList.filterDeletedPresets().map { it.asPhrase() } }
    }

    override suspend fun getPhrase(phraseId: String): PresetPhrase? {
        return presetPhrasesDao.getPhrase(phraseId)?.asPhrase()
    }

    override suspend fun deletePhrase(phraseId: String) {
        presetPhrasesDao.deletePhrase(phraseId, deleted = true)
    }

    private fun List<PresetPhraseDto>.filterDeletedPresets(): List<PresetPhraseDto> {
        return filterNot { it.deleted }
    }

    private suspend fun ensurePopulated() {
        phrasesMutex.withLock {
            val existingPresetPhrases = presetPhrasesDao.getAllPresetPhrases()
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
        presetPhrasesDao.insertPhrases(presetPhrases)
    }
}