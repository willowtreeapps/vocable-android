package com.willowtree.vocable.data.repository

import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

interface StoredPhrasesRepository {
    suspend fun addPhrase(phrase: PhraseDto)
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
    fun getRecentPhrasesFlow(): Flow<List<Phrase>>
    fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>>
    suspend fun getPhrase(phraseId: String): Phrase?
    suspend fun updatePhrase(phrase: PhraseDto)
    suspend fun updatePhraseLocalizedUtterance(
        phraseId: String,
        localizedUtterance: LocalesWithText,
    )
    suspend fun deletePhrase(phraseId: String)
}