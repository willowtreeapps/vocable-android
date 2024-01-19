package com.willowtree.vocable

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalesWithText

interface IPhrasesUseCase {

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>

    suspend fun phraseSpoken(phraseId: Long)

    suspend fun deletePhrase(phraseId: Long)

    suspend fun updatePhrase(phraseId: Long, localizedUtterance: LocalesWithText)

    suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String)
}