package com.willowtree.vocable

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalesWithText

interface IPhrasesUseCase {

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>

    suspend fun updatePhraseLastSpokenTime(phraseId: String)

    suspend fun deletePhrase(phraseId: String)

    suspend fun updatePhrase(phraseId: String, localizedUtterance: LocalesWithText)

    suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String)
}