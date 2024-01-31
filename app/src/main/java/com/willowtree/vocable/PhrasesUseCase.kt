package com.willowtree.vocable

import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.PresetPhrasesRepository
import com.willowtree.vocable.room.StoredPhrasesRepository
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.locale.LocalesWithText

class PhrasesUseCase(
    private val legacyPhrasesRepository: ILegacyCategoriesAndPhrasesRepository,
    private val storedPhrasesRepository: StoredPhrasesRepository,
    private val presetPhrasesRepository: PresetPhrasesRepository,
    private val dateProvider: DateProvider,
) : IPhrasesUseCase {
    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        if (categoryId == PresetCategories.RECENTS.id) {
            return legacyPhrasesRepository.getRecentPhrases().map { it.asPhrase() }
        }
        return legacyPhrasesRepository.getPhrasesForCategory(categoryId).map { it.asPhrase() }
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        storedPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
        presetPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
    }

    override suspend fun deletePhrase(phraseId: String) {
        legacyPhrasesRepository.deletePhrase(phraseId)
    }

    override suspend fun updatePhrase(phraseId: String, localizedUtterance: LocalesWithText) {
        legacyPhrasesRepository.updatePhrase(phraseId, localizedUtterance)
    }

    override suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String) {
        legacyPhrasesRepository.addPhrase(PhraseDto(
            phraseId = 0L,
            parentCategoryId = parentCategoryId,
            creationDate = dateProvider.currentTimeMillis(),
            lastSpokenDate = null,
            localizedUtterance = localizedUtterance,
            sortOrder = legacyPhrasesRepository.getPhrasesForCategory(parentCategoryId).size
        ))
    }
}