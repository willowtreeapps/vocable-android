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
     * Resets every preset category (General, Basic Needs, Personal Care, Conversation,
     * Environment, the 123 keypad) back to exactly its default phrase set: undoes edits
     * ("shadows"), restores individually deleted preset phrases, and removes any custom phrases
     * added to those categories - a preset category's default state is the array it was seeded
     * from, and nothing a user did to it since has a "default" of its own to fall back to.
     * Categories with no such default at all - Recents (derived, not a real store) and My Sayings
     * (a pure user-favorites bucket) - are left untouched, as is every genuinely user-created
     * category. Unlike [resetToDefaults] (the "Reset Everything" nuclear option, which also wipes
     * user-created categories' phrases), this backs the Reset App Settings screen's standalone
     * "Phrases" domain reset.
     */
    suspend fun resetPresetPhrasesToDefaults()
}