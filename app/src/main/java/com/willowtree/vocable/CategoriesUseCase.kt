package com.willowtree.vocable

import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryDto
import kotlinx.coroutines.flow.Flow

class CategoriesUseCase(
    private val presetsRepository: PresetsRepository
) {

    fun categories(): Flow<List<CategoryDto>> {
        return presetsRepository.getAllCategoriesFlow()
    }

}