package com.willowtree.vocable.domain.usecase

import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

interface IPhrasesUseCase {

    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>

    fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>>

    suspend fun updatePhraseLastSpokenTime(phraseId: String)

    suspend fun deletePhrase(phraseId: String)

    suspend fun updatePhrase(phraseId: String, updatedPhrase: String)

    suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String)

    suspend fun resetToDefaults()

    /**
     * Resets only this category's phrases: removes any custom/edited phrases in it and restores
     * its preset phrases (if it has any), without touching other categories.
     */
    suspend fun resetPhrasesForCategory(categoryId: String)

    /**
     * Resets every preset-derived phrase across every preset category back to its default text,
     * undoing edits ("shadows") and restoring any individually deleted preset phrases - without
     * touching genuinely custom phrases anywhere, whether added to a preset category or living in
     * a user-created category. Unlike [resetToDefaults] (the "Reset Everything" nuclear option,
     * which wipes every phrase including custom ones), this backs the Reset App Settings screen's
     * standalone "Phrases" domain reset, where "reset to default" should only apply to phrases
     * that actually have a default to go back to.
     */
    suspend fun resetPresetPhrasesToDefaults()
}