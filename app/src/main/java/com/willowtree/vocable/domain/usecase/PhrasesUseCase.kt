package com.willowtree.vocable.domain.usecase

import com.willowtree.vocable.domain.model.CustomPhrase
import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.domain.model.PresetCategories
import com.willowtree.vocable.domain.model.PresetPhrase
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.data.repository.PresetPhrasesRepository
import com.willowtree.vocable.data.repository.StoredPhrasesRepository
import com.willowtree.vocable.core.DateProvider
import com.willowtree.vocable.core.UUIDProvider
import com.willowtree.vocable.core.locale.LocaleProvider
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class PhrasesUseCase(
    private val storedPhrasesRepository: StoredPhrasesRepository,
    private val presetPhrasesRepository: PresetPhrasesRepository,
    private val dateProvider: DateProvider,
    private val uuidProvider: UUIDProvider,
    private val localeProvider: LocaleProvider
) : IPhrasesUseCase {
    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return getPhrasesForCategoryFlow(categoryId).first()
    }

    override fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>> {
        return combine(
            presetPhrasesRepository.getRecentPhrasesFlow(),
            storedPhrasesRepository.getRecentPhrasesFlow(),
            storedPhrasesRepository.getPhrasesForCategoryFlow(categoryId),
            presetPhrasesRepository.getPhrasesForCategoryFlow(categoryId)
        ) { recentPresets, recentStored, stored, presets ->
            if (categoryId == PresetCategories.RECENTS.id) {
                (recentPresets + recentStored)
                    .sortedByDescending { it.lastSpokenDate }
                    .take(8)
            } else {
                stored + presets
            }
        }
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        storedPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
        presetPhrasesRepository.updatePhraseLastSpokenTime(phraseId)
    }

    override suspend fun deletePhrase(phraseId: String) {
        storedPhrasesRepository.deletePhrase(phraseId)
        presetPhrasesRepository.deletePhrase(phraseId)
    }

    override suspend fun updatePhrase(phraseId: String, updatedPhrase: String) {
        val phrase = storedPhrasesRepository.getPhrase(phraseId)
            ?: presetPhrasesRepository.getPhrase(phraseId)
                .takeIf { it != null && !it.deleted }!!
        when (phrase) {
            is CustomPhrase -> {
                val localizedUtterance = (phrase.localizedUtterance ?: LocalesWithText(emptyMap()))
                    .with(localeProvider.getDefaultLocaleString(), updatedPhrase)
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
                        localizedUtterance = LocalesWithText(mapOf(localeProvider.getDefaultLocaleString() to updatedPhrase)),
                        sortOrder = phrase.sortOrder
                    )
                )

            }
        }
    }

    override suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String) {
        if (parentCategoryId != PresetCategories.RECENTS.id) {
            storedPhrasesRepository.addPhrase(
                PhraseDto(
                    phraseId = uuidProvider.randomUUIDString(),
                    parentCategoryId = parentCategoryId,
                    creationDate = dateProvider.currentTimeMillis(),
                    lastSpokenDate = null,
                    localizedUtterance = localizedUtterance,
                    sortOrder = storedPhrasesRepository.getPhrasesForCategoryFlow(parentCategoryId).first().size
                )
            )
        } else {
            throw Exception(
                "The 'Recents' category is not a true category -" +
                        " it is a filter applied to true categories. Therefore, saving phrases from " +
                        "the Recents 'category' is not supported."
            )
        }
    }
}