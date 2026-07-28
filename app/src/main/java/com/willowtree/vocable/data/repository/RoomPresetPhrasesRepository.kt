package com.willowtree.vocable.data.repository

import android.content.Context
import com.willowtree.vocable.data.room.PhraseDao
import com.willowtree.vocable.data.room.PhraseSpokenDate
import com.willowtree.vocable.data.room.PresetPhraseDto
import com.willowtree.vocable.data.room.PresetPhrasesDao
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.domain.model.PresetPhrase
import com.willowtree.vocable.domain.model.asPhrase
import com.willowtree.vocable.core.DateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext.get
import kotlin.collections.map

class RoomPresetPhrasesRepository(
    private val presetPhrasesDao: PresetPhrasesDao,
    private val phraseDao: PhraseDao,
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
            val resources = get().get<Context>().resources
            val existingSortOrders = presetPhrasesDao.getAllPresetPhrases()
                .associate { it.phraseId to it.sortOrder }

            PresetCategories.values().forEach { presetCategory ->
                if (presetCategory != PresetCategories.RECENTS && presetCategory != PresetCategories.MY_SAYINGS) {
                    val phrasesIds = resources.obtainTypedArray(presetCategory.getArrayId())
                    val phraseObjects = mutableListOf<PresetPhraseDto>()
                    for (index in 0 until phrasesIds.length()) {
                        val phraseId = phrasesIds.getResourceId(index, 0)
                        val phraseEntryName = resources.getResourceEntryName(phraseId)
                        val existingSortOrder = existingSortOrders[phraseEntryName]
                        if (existingSortOrder == null) {
                            phraseObjects.add(
                                PresetPhraseDto(
                                    phraseId = phraseEntryName,
                                    parentCategoryId = presetCategory.id,
                                    creationDate = System.currentTimeMillis(),
                                    lastSpokenDate = null,
                                    sortOrder = index,
                                )
                            )
                        } else if (existingSortOrder != index) {
                            // The array is the source of truth for display order, but a phrase
                            // seeded by an earlier release keeps whatever sort_order it was given
                            // then - resync it so reordering an array actually takes effect.
                            presetPhrasesDao.updatePhraseSortOrder(phraseEntryName, index)
                            // Editing a preset leaves a stored "shadow" phrase behind that reuses
                            // the preset's id and the sort_order captured at edit time. Renumber
                            // it in step, or the two disagree and the category renders with a
                            // duplicated sort order - and one phrase too many for its page.
                            phraseDao.updatePhraseSortOrder(phraseEntryName, index)
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