package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.asCategory
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.StoredCategoriesRepository
import com.willowtree.vocable.utils.UUIDProvider
import com.willowtree.vocable.utils.locale.LocaleProvider
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CategoriesUseCase(
    private val uuidProvider: UUIDProvider,
    private val localeProvider: LocaleProvider,
    private val storedCategoriesRepository: StoredCategoriesRepository,
    private val presetCategoriesRepository: PresetCategoriesRepository,
    private val phrasesUseCase: PhrasesUseCase
) : ICategoriesUseCase {

    override fun categories(): Flow<List<Category>> =
        combine(
            storedCategoriesRepository.getAllCategories(),
            presetCategoriesRepository.getPresetCategories()
        ) { storedCategories, presetCategories ->
            storedCategories.map { it.asCategory() } + presetCategories
        }

    override suspend fun getCategoryById(categoryId: String): Category {
        return storedCategoriesRepository.getCategoryById(categoryId)?.asCategory()
            ?: presetCategoriesRepository.getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category with id $categoryId not found")
    }

    override suspend fun updateCategoryName(
        categoryId: String,
        localizedName: LocalesWithText
    ) {
        val storedCategory = storedCategoriesRepository.getCategoryById(categoryId)
        if (storedCategory != null) {
            storedCategoriesRepository.upsertCategory(
                Category.StoredCategory(
                    storedCategory.categoryId,
                    localizedName,
                    storedCategory.hidden,
                    storedCategory.sortOrder
                )
            )
        } else {
            val presetCategory = presetCategoriesRepository.getCategoryById(categoryId)
            if (presetCategory != null) {
                presetCategoriesRepository.deleteCategory(categoryId)
                storedCategoriesRepository.upsertCategory(
                    Category.StoredCategory(
                        presetCategory.categoryId,
                        localizedName,
                        presetCategory.hidden,
                        presetCategory.sortOrder
                    )
                )
            } else {
                throw IllegalArgumentException("Category with id $categoryId not found")
            }
        }
    }

    override suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        presetCategoriesRepository.updateCategoryHidden(categoryId, hidden)
        storedCategoriesRepository.updateCategoryHidden(categoryId, hidden)
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        presetCategoriesRepository.updateCategorySortOrders(categorySortOrders)
        storedCategoriesRepository.updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun addCategory(categoryName: String, sortOrder: Int) {
        storedCategoriesRepository.upsertCategory(
            Category.StoredCategory(
                uuidProvider.randomUUIDString(),
                LocalesWithText(mapOf(Pair(localeProvider.getDefaultLocaleString(), categoryName))),
                false,
                sortOrder
            )
        )
    }

    override suspend fun deleteCategory(categoryId: String) {
        phrasesUseCase.getPhrasesForCategory(categoryId).forEach {
            phrasesUseCase.deletePhrase(it.phraseId)
        }
        storedCategoriesRepository.deleteCategory(categoryId)
        presetCategoriesRepository.deleteCategory(categoryId)
    }

}