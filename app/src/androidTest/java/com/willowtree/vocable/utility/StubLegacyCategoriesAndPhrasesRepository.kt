package com.willowtree.vocable.utility

import com.willowtree.vocable.core.locale.LocalesWithText
import com.willowtree.vocable.data.room.CategoryDto
import com.willowtree.vocable.data.room.CategorySortOrder
import com.willowtree.vocable.data.room.PhraseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Legacy stub retained only to avoid stale test compile failures during migration.
// The legacy repository interface no longer exists in production code.
class StubLegacyCategoriesAndPhrasesRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto> = error("Not implemented")

    fun getAllCategoriesFlow(): Flow<List<CategoryDto>> = flowOf(emptyList())

    suspend fun getAllCategories(): List<CategoryDto> = emptyList()

    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) = Unit

    suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText) = Unit

    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) = Unit

    suspend fun deleteCategory(categoryId: String) = Unit

    suspend fun getRecentPhrases(): List<PhraseDto> = emptyList()

    suspend fun deletePhrases(phrases: List<PhraseDto>) = Unit
}
