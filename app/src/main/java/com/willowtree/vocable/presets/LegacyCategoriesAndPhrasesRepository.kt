package com.willowtree.vocable.presets

import android.content.Context
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.CategoryLocalizedName
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.room.VocableDatabase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent

class LegacyCategoriesAndPhrasesRepository(
    val context: Context,
    private val database: VocableDatabase
) : KoinComponent, ILegacyCategoriesAndPhrasesRepository {

    override suspend fun getAllCategories(): List<CategoryDto> {
        return database.categoryDao().getAllCategories()
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryDto>> {
        return database.categoryDao().getAllCategoriesFlow()
    }

    override suspend fun getPhrasesForCategory(categoryId: String): List<PhraseDto> {
        return database.phraseDao().getPhrasesForCategory(categoryId)
    }

    override suspend fun getRecentPhrases(): List<PhraseDto> =
        database.phraseDao().getRecentPhrases()

    private suspend fun populatePhrases(phrases: List<PhraseDto>) {
        database.phraseDao().insertPhrases(*phrases.toTypedArray())
    }

    override suspend fun deletePhrase(phraseId: String) {
        database.phraseDao().deletePhrase(phraseId)
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        database.categoryDao().updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun deletePhrases(phrases: List<PhraseDto>) {
        database.phraseDao().deletePhrases(*phrases.toTypedArray())
    }

    override suspend fun deleteCategory(categoryId: String) {
        database.categoryDao().deleteCategory(categoryId)
    }

    override suspend fun updateCategoryName(categoryId: String, localizedName: LocalesWithText) {
        database.categoryDao().updateCategory(CategoryLocalizedName(categoryId, localizedName))
    }

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        TODO("Not yet implemented")
    }
}
