package com.willowtree.vocable

import com.willowtree.vocable.domain.model.Phrase
import com.willowtree.vocable.domain.model.asPhrase
import com.willowtree.vocable.data.room.CategoryDto
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakePhrasesUseCase : IPhrasesUseCase {

    val _allCategories = MutableStateFlow(
        listOf(
            CategoryDto(
                categoryId = "1",
                creationDate = 0L,
                localizedName = LocalesWithText(mapOf("en_US" to "category")),
                hidden = false,
                sortOrder = 0
            )
        )
    )

    var _categoriesToPhrases = mapOf(
        "1" to listOf(
            PhraseDto(
                phraseId = "1",
                parentCategoryId = "1",
                creationDate = 0L,
                lastSpokenDate = null,
                localizedUtterance = LocalesWithText(mapOf("en_US" to "Hello")),
                sortOrder = 0
            )
        )
    )

    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return _categoriesToPhrases[categoryId].orEmpty().map { it.asPhrase() }
    }

    override fun getPhrasesForCategoryFlow(categoryId: String): Flow<List<Phrase>> {
        return flowOf(_categoriesToPhrases[categoryId].orEmpty().map { it.asPhrase() })
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        error("Not implemented")
    }

    override suspend fun deletePhrase(phraseId: String) = error("Not implemented")

    override suspend fun updatePhrase(phraseId: String, updatedPhrase: String) {
        error("Not implemented")
    }

    override suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String) {
        error("Not implemented")
    }
}
