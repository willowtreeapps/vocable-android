package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.ILegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.asCategory
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.StoredCategoriesRepository
import com.willowtree.vocable.utils.UUIDProvider
import com.willowtree.vocable.utils.locale.LocaleProvider
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoriesUseCase(
    private val legacyCategoriesAndPhrasesRepository: ILegacyCategoriesAndPhrasesRepository,
    private val uuidProvider: UUIDProvider,
    private val localeProvider: LocaleProvider,
    private val storedCategoriesRepository: StoredCategoriesRepository,
    private val presetCategoriesRepository: PresetCategoriesRepository
) : ICategoriesUseCase {

    override fun categories(): Flow<List<Category>> {
        return storedCategoriesRepository.getAllCategories()
            .map { categoryDtoList ->
                categoryDtoList.map { it.asCategory() } + presetCategoriesRepository.getPresetCategories()
            }
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
        legacyCategoriesAndPhrasesRepository.updateCategoryName(categoryId, localizedName)
    }

    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        legacyCategoriesAndPhrasesRepository.updateCategoryHidden(categoryId, hidden)
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        presetCategoriesRepository.updateCategorySortOrders(categorySortOrders)
        storedCategoriesRepository.updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun addCategory(categoryName: String, sortOrder: Int) {
        storedCategoriesRepository.addCategory(
            Category.StoredCategory(
                uuidProvider.randomUUIDString(),
                null,
                LocalesWithText(mapOf(Pair(localeProvider.getDefaultLocaleString(), categoryName))),
                false,
                sortOrder
            )
        )
    }

    suspend fun deleteCategory(categoryId: String) {
        legacyCategoriesAndPhrasesRepository.deleteCategory(categoryId)
    }

}