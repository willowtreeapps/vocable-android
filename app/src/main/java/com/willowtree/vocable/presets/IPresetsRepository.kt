package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.flow.Flow

//TODO: PK - Rename this once we make the jump to rename [PresetsRepository] -> "RoomPresetsRepository"
interface IPresetsRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>
    suspend fun addPhraseToRecents(phrase: Phrase)

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryDto>>

    /**
     * Return all categories, sorted by [CategoryDto.sortOrder]
     */
    suspend fun getAllCategories(): List<CategoryDto>
    suspend fun deletePhrase(phrase: Phrase)
    suspend fun updateCategories(categories: List<CategoryDto>)
    suspend fun updateCategory(category: CategoryDto)
    suspend fun addCategory(category: CategoryDto)
}