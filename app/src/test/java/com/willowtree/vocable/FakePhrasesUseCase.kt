package com.willowtree.vocable

import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.asPhrase
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.MutableStateFlow

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
        return _categoriesToPhrases[categoryId]!! // go ahead and blow up if our test data isn't valid
            .map { it.asPhrase() }
    }

    override suspend fun updatePhraseLastSpokenTime(phraseId: String) {
        error("Not implemented")
    }

    override suspend fun deletePhrase(phraseId: String) = error("Not implemented")

    override suspend fun updatePhrase(phraseId: String, localizedUtterance: LocalesWithText) {
        error("Not implemented")
    }

    override suspend fun addPhrase(localizedUtterance: LocalesWithText, parentCategoryId: String) {
        error("Not implemented")
    }

}