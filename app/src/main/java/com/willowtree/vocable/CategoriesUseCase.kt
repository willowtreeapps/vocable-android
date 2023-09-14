package com.willowtree.vocable

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.asCategory
import com.willowtree.vocable.presets.asDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoriesUseCase(
    private val presetsRepository: IPresetsRepository
) {

    fun categories(): Flow<List<Category>> {
        return presetsRepository.getAllCategoriesFlow()
            .map { categoryDtoList -> categoryDtoList.map { it.asCategory() } }
    }

    suspend fun getCategoryById(categoryId: String): Category =
        presetsRepository.getCategoryById(categoryId).asCategory()

    suspend fun updateCategory(category: Category) {
        presetsRepository.updateCategory(category.asDto())
    }

    suspend fun updateCategories(categories: List<Category>) {
        presetsRepository.updateCategories(categories.map { it.asDto() })
    }

    suspend fun addCategory(category: Category) {
        presetsRepository.addCategory(category.asDto())
    }

    suspend fun deleteCategory(categoryId: String) {
        presetsRepository.deleteCategory(categoryId)
    }

}