package com.willowtree.vocable.room

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetPhrase

interface PresetPhrasesRepository {
    suspend fun populateDatabase()
    suspend fun getAllPresetPhrases(): List<PresetPhrase>
    suspend fun updatePhraseLastSpokenTime(phraseId: String)
    suspend fun getRecentPhrases() : List<PresetPhrase>
    suspend fun getPhrasesForCategory(categoryId: String): List<PresetPhrase>
    suspend fun getPhrase(phraseId: String): Phrase?
    suspend fun updatePhraseHidden(phraseId: String, hidden: Boolean)
}