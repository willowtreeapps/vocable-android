package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePresetsRepository : IPresetsRepository {

    val _allCategories = MutableStateFlow(
        listOf(
            CategoryDto(
                categoryId = "1",
                creationDate = 0L,
                resourceId = null,
                localizedName = mapOf("en_US" to "category"),
                hidden = false,
                sortOrder = 0
            )
        )
    )

    var _categoriesToPhrases = mapOf(
        "1" to listOf(
            Phrase(
                phraseId = 1L,
                parentCategoryId = "1",
                creationDate = 0L,
                lastSpokenDate = 0L,
                localizedUtterance = mapOf("en_US" to "Hello"),
                sortOrder = 0
            )
        )
    )

    override suspend fun getPhrasesForCategory(categoryId: String): List<Phrase> {
        return _categoriesToPhrases[categoryId]!! // go ahead and blow up if our test data isn't valid
    }

    override suspend fun addPhraseToRecents(phrase: Phrase) {
        TODO("Not yet implemented")
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return _allCategories
    }

    override suspend fun getAllCategories(): List<CategoryDto> {
        return _allCategories.value
    }

    override suspend fun deletePhrase(phrase: Phrase) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCategories(categories: List<CategoryDto>) {
        TODO("Not yet implemented")
    }
}