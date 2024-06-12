package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

interface StoredPhrasesRepository {
    suspend fun addPhrase(phrase: PhraseDto)
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
    suspend fun getRecentPhrases(): List<Phrase>
    fun getRecentPhrasesFlow(): Flow<List<Phrase>>
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>
    fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>>
    suspend fun getPhrase(phraseId: String): Phrase?
    suspend fun updatePhrase(phrase: PhraseDto)
    suspend fun updatePhraseLocalizedUtterance(
        phraseId: String,
        localizedUtterance: LocalesWithText,
    )
    suspend fun deletePhrase(phraseId: String)
}