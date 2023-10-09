package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.PresetCategoriesRepository
import com.willowtree.vocable.presets.asCategory
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.room.StoredCategoriesRepository
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.LocaleProvider
import com.willowtree.vocable.utils.UUIDProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoriesUseCase(
    private val presetsRepository: IPresetsRepository,
    private val uuidProvider: UUIDProvider,
    private val dateProvider: DateProvider,
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

    suspend fun getCategoryById(categoryId: String): Category =
        presetsRepository.getCategoryById(categoryId).asCategory()

    override suspend fun updateCategoryName(
        categoryId: String,
        localizedName: Map<String, String>
    ) {
        presetsRepository.updateCategoryName(categoryId, localizedName)
    }

    suspend fun updateCategoryHidden(categoryId: String, hidden: Boolean) {
        presetsRepository.updateCategoryHidden(categoryId, hidden)
    }

    override suspend fun updateCategorySortOrders(categorySortOrders: List<CategorySortOrder>) {
        presetsRepository.updateCategorySortOrders(categorySortOrders)
    }

    override suspend fun addCategory(categoryName: String, sortOrder: Int) {
        presetsRepository.addCategory(
            CategoryDto(
                uuidProvider.randomUUIDString(),
                dateProvider.currentTimeMillis(),
                null,
                mapOf(Pair(localeProvider.getDefaultLocaleString(), categoryName)),
                false,
                sortOrder
            )
        )
    }

    suspend fun deleteCategory(categoryId: String) {
        presetsRepository.deleteCategory(categoryId)
    }

}