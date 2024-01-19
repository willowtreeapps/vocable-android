package com.willowtree.vocable

import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.locale.LocalesWithText

class PhrasesUseCase(
    private val legacyPhrasesRepository: ILegacyCategoriesAndPhrasesRepository,
    private val dateProvider: DateProvider,
) : IPhrasesUseCase {
    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        if (categoryId == PresetCategories.RECENTS.id) {
            return legacyPhrasesRepository.getRecentPhrases().map { it.asPhrase() }
        }
        return legacyPhrasesRepository.getPhrasesForCategory(categoryId).map { it.asPhrase() }
    }

    override suspend fun phraseSpoken(phraseId: Long) {
        legacyPhrasesRepository.updatePhraseLastSpoken(phraseId, dateProvider.currentTimeMillis())
    }

    override suspend fun deletePhrase(phraseId: Long) {
        legacyPhrasesRepository.deletePhrase(phraseId)
    }

    override suspend fun updatePhrase(phraseId: Long, localizedUtterance: LocalesWithText) {
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