package com.willowtree.vocable

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

interface IPhrasesUseCase {

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>

    fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>>

    suspend fun updatePhraseLastSpokenTime(phraseId: String)

    suspend fun deletePhrase(phraseId: String)

    suspend fun updatePhrase(phraseId: String, updatedPhrase: String)

    suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String)
}