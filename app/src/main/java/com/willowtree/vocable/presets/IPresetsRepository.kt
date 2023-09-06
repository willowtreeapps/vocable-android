package com.willowtree.vocable.presets

import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.flow.Flow

//TODO: PK - Rename this once we make the jump to rename [PresetsRepository] -> "RoomPresetsRepository"
interface IPresetsRepository {
    suspend fun getPhrasesForCategory(categoryId: String): List<Phrase>
    suspend fun addPhraseToRecents(phrase: Phrase)
    fun getAllCategoriesFlow(): Flow<List<CategoryDto>>
    suspend fun getAllCategories(): List<CategoryDto>
    suspend fun deletePhrase(phrase: Phrase)
    suspend fun updateCategories(categories: List<CategoryDto>)
}