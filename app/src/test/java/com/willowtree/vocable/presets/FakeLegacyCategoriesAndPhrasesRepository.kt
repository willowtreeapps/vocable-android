package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLegacyCategoriesAndPhrasesRepository : ILegacyCategoriesAndPhrasesRepository {

    val _allCategories = MutableStateFlow(
        listOf(
            CategoryDto(
                categoryId = "1",
                creationDate = 0L,
                resourceId = null,
                localizedName = LocalesWithText( mapOf("en_US" to "category")),
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
                localizedUtterance =LocalesWithText(  mapOf("en_US" to "Hello")),
                sortOrder = 0
            )
        )
    )

    var _recentPhrases = listOf(
        PhraseDto(
            phraseId = "1",
            parentCategoryId = "1",
            creationDate = 0L,
            lastSpokenDate = null,
            localizedUtterance = null,
            sortOrder = 0
        )
    )

    override suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto> {
        return _categoriesToPhrases[categoryId]!! // go ahead and blow up if our test data isn't valid
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return _allCategories.map { categoryDtos -> categoryDtos.sortedBy { it.sortOrder } }
    }

    override suspend fun getAllCategories(): List<CategoryDto> {
        return _allCategories.value.sortedBy { it.sortOrder }
    }

    override suspend fun deletePhrase(phraseId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        _allCategories.update { allCategories ->
            allCategories.map { categoryDto ->
                val sortOrderUpdate =
                    categorySortOrders.firstOrNull { it.categoryId == categoryDto.categoryId }
                if (sortOrderUpdate != null) {
                    categoryDto.copy(sortOrder = sortOrderUpdate.sortOrder)
                } else {
                    categoryDto
                }
            }
        }
    }

    override suspend fun updateCategoryName(
        categoryId: String,
        localizedName: LocalesWithText
    ) {
        _allCategories.update { allCategories ->
            allCategories.map {
                if (it.categoryId == categoryId) {
                    it.copy(localizedName = localizedName)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCategory(categoryId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getRecentPhrases(): List<PhraseDto> = _recentPhrases
}