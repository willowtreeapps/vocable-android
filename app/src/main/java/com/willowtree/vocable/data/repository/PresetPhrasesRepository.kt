package com.willowtree.vocable.data.repository

import com.willowtree.vocable.domain.model.PresetPhrase
import kotlinx.coroutines.flow.Flow

interface PresetPhrasesRepository {
    suspend fun populateDatabase()
    suspend fun getAllPresetPhrases(): List<PresetPhrase>
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
    suspend fun getRecentPhrases() : List<PresetPhrase>
    fun getRecentPhrasesFlow(): Flow<List<PresetPhrase>>
    suspend fun getPhrasesForCategory(categoryId: String): List<PresetPhrase>
    fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<PresetPhrase>>
    suspend fun getPhrase(phraseId: String): PresetPhrase?
    suspend fun deletePhrase(phraseId: String)
}