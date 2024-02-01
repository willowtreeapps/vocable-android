package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Phrase

interface StoredPhrasesRepository {
    suspend fun addPhrase(phrase: PhraseDto)
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
    suspend fun getRecentPhrases(): List<Phrase>
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>
}