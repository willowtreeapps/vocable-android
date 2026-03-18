package com.willowtree.vocable.presets

import com.willowtree.vocable.data.room.CategoryDto
import com.willowtree.vocable.data.room.CategorySortOrder
import com.willowtree.vocable.data.room.PhraseDto
import com.willowtree.vocable.core.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Legacy fake kept only for older tests. The original shared interface no longer exists,
 * so this fake now exposes the same API directly without implementing that removed type.
 */
class FakeLegacyCategoriesAndPhrasesRepository {

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

    suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto> {
        return _categoriesToPhrases[categoryId]!! // go ahead and blow up if our test data isn't valid
    }

    fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return _allCategories.map { categoryDtos -> categoryDtos.sortedBy { it.sortOrder } }
    }

    suspend fun getAllCategories(): List<CategoryDto> {
        return _allCategories.value.sortedBy { it.sortOrder }
    }

    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
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

    suspend fun updateCategoryName(
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

    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        _allCategories.update { allCategories ->
            allCategories.map {
                if (it.categoryId == categoryId) {
                    it.copy(hidden = hidden)
                } else {
                    it
                }
            }
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        _allCategories.update { categories ->
            categories.filterNot { it.categoryId == categoryId }
        }
        _categoriesToPhrases = _categoriesToPhrases - categoryId
    }

    suspend fun getRecentPhrases(): List<PhraseDto> = _recentPhrases

    suspend fun deletePhrases(phrases: List<PhraseDto>) {
        val phraseIds = phrases.map { it.phraseId }.toSet()
        _categoriesToPhrases = _categoriesToPhrases.mapValues { (_, categoryPhrases) ->
            categoryPhrases.filterNot { it.phraseId in phraseIds }
        }
        _recentPhrases = _recentPhrases.filterNot { it.phraseId in phraseIds }
    }
}
