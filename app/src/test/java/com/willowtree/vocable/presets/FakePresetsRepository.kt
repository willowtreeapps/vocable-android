package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
        return _allCategories.map { categoryDtos -> categoryDtos.sortedBy { it.sortOrder } }
    }

    override suspend fun getAllCategories(): List<CategoryDto> {
        return _allCategories.value.sortedBy { it.sortOrder }
    }

    override suspend fun deletePhrase(phrase: Phrase) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCategories(updatedCategories: List<CategoryDto>) {
        val allCategories = _allCategories.value.map { originalDto ->
            updatedCategories.find { it.categoryId == originalDto.categoryId }
                ?: originalDto
        }
        _allCategories.update { allCategories }
    }

    override suspend fun updateCategory(updatedCategory: CategoryDto) {
        updateCategories(listOf(updatedCategory))
    }

    override suspend fun addCategory(category: CategoryDto) {
        _allCategories.update { it + category }
    }
}