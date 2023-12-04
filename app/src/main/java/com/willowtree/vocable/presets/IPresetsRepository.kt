package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow

//TODO: PK - Rename this once we make the jump to rename [PresetsRepository] -> "RoomPresetsRepository"
interface IPresetsRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto>

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryDto>>

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    suspend fun getAllCategories(): List<CategoryDto>
    suspend fun deletePhrase(phraseId: Long)
    suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>)
    suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText)
    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean)
    suspend fun getCategoryById(categoryId: String): CategoryDto
    suspend fun deleteCategory(categoryId: String)
    suspend fun getRecentPhrases(): List<PhraseDto>
    suspend fun updatePhraseLastSpoken(phraseId: Long, lastSpokenDate: Long)
    suspend fun updatePhrase(phraseId: Long, localizedUtterance: LocalesWithText)
    suspend fun addPhrase(phrase: PhraseDto)
}