package com.willowtree.vocable

import com.willowtree.vocable.presets.CustomPhrase
import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetPhrase
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.PresetPhrasesRepository
import com.willowtree.vocable.room.StoredPhrasesRepository
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.UUIDProvider
import com.willowtree.vocable.utils.locale.LocalesWithText

class PhrasesUseCase(
    private val legacyPhrasesRepository: ILegacyCategoriesAndPhrasesRepository,
    private val storedPhrasesRepository: StoredPhrasesRepository,
    private val presetPhrasesRepository: PresetPhrasesRepository,
    private val dateProvider: DateProvider,
    private val uuidProvider: UUIDProvider,
) : IPhrasesUseCase {
    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        if (categoryId == PresetCategories.RECENTS.id) {
            val presets = presetPhrasesRepository.getRecentPhrases()
            val stored = storedPhrasesRepository.getRecentPhrases()
            return (presets + stored).sortedByDescending { it.lastSpokenDate }.take(8)
        }
        return storedPhrasesRepository.getPhrasesForCategory(categoryId) +
                presetPhrasesRepository.getPhrasesForCategory(categoryId)
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        storedPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
        presetPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
    }

    override suspend fun deletePhrase(phraseId: String) {
        storedPhrasesRepository.deletePhrase(phraseId)
        presetPhrasesRepository.deletePhrase(phraseId)
    }

    override suspend fun updatePhrase(phraseId: String, localizedUtterance: LocalesWithText) {
        val phrase = storedPhrasesRepository.getPhrase(phraseId)
            ?: presetPhrasesRepository.getPhrase(phraseId)
                .takeIf { it != null && !it.deleted }
        when (phrase) {
            is CustomPhrase -> {
                storedPhrasesRepository.updatePhraseLocalizedUtterance(
                    phraseId = phraseId,
                    localizedUtterance = localizedUtterance,
                )
            }
            is PresetPhrase -> {
                presetPhrasesRepository.deletePhrase(phraseId = phraseId)
                // add a custom phrase to "shadow" over the preset
                storedPhrasesRepository.addPhrase(
                    PhraseDto(
                        phraseId = phrase.phraseId,
                        parentCategoryId = phrase.parentCategoryId,
                        creationDate = dateProvider.currentTimeMillis(),
                        lastSpokenDate = phrase.lastSpokenDate,
                        localizedUtterance = localizedUtterance,
                        sortOrder = phrase.sortOrder
                    )
                )

            }
            null -> throw IllegalArgumentException("Phrase with id $phraseId not found")
        }
    }

    override suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String) {
        if (parentCategoryId != PresetCategories.RECENTS.id) {
            storedPhrasesRepository.addPhrase(PhraseDto(
                phraseId = uuidProvider.randomUUIDString(),
                parentCategoryId = parentCategoryId,
                creationDate = dateProvider.currentTimeMillis(),
                lastSpokenDate = null,
                localizedUtterance = localizedUtterance,
                sortOrder = legacyPhrasesRepository.getPhrasesForCategory(parentCategoryId).size
            ))
        } else {
            throw Exception("The 'Recents' category is not a true category -" +
                    " it is a filter applied to true categories. Therefore, saving phrases from " +
                    "the Recents 'category' is not supported.")
        }
    }
}