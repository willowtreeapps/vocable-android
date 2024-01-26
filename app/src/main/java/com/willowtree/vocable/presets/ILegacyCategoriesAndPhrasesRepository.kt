package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

@Deprecated("This is the old way of accessing categories and phrases. Prefer using" +
        " ICategoriesUseCase and PhrasesUseCase instead.")
interface ILegacyCategoriesAndPhrasesRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto>

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryDto>>

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    suspend fun getAllCategories(): List<CategoryDto>
    suspend fun deletePhrase(phraseId: String)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText)
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
    suspend fun deleteCategory(categoryId: String)
    suspend fun getRecentPhrases(): List<PhraseDto>
    suspend fun updatePhraseLastSpoken(phraseId: String, lastSpokenDate: Long)
    suspend fun updatePhrase(phraseId: String, localizedUtterance: LocalesWithText)
    suspend fun addPhrase(phrase: PhraseDto)
}