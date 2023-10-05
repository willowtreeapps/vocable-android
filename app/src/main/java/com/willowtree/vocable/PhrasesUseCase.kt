package com.willowtree.vocable

import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.utils.DateProvider

class PhrasesUseCase(
    private val presetsRepository: IPresetsRepository,
    private val dateProvider: DateProvider
) {
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        if (categoryId == PresetCategories.RECENTS.id) {
            return presetsRepository.getRecentPhrases().map { it.asPhrase() }
        }
        return presetsRepository.getPhrasesForCategory(categoryId).map { it.asPhrase() }
    }

    suspend fun phraseSpoken(phraseId: Long) {
        presetsRepository.updatePhraseLastSpoken(phraseId, dateProvider.currentTimeMillis())
    }
}