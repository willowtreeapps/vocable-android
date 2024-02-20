package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditCategoryMenuViewModel(
    private val categoriesUseCase: ICategoriesUseCase
) : ViewModel() {

    // TODO: PK - Does this need to be LiveData? Is it updating?
    private val _currentCategory = MutableLiveData<Category>()
    val currentCategory: LiveData<Category> = _currentCategory

    private val _lastCategoryRemaining = MutableLiveData<Boolean>()
    val lastCategoryRemaining: LiveData<Boolean> = _lastCategoryRemaining

    private val _popBackStack = MutableStateFlow(false)
    val popBackStack = _popBackStack.asStateFlow()

    //TODO: PK - Can this be moved to `init`, and the id can be fetched from nav args?
    fun updateCategoryById(categoryId: String) {
        viewModelScope.launch {
            _lastCategoryRemaining.postValue(categoriesUseCase.categories().first().size == 1)
            _currentCategory.postValue(categoriesUseCase.getCategoryById(categoryId))
        }
    }

    fun updateCategoryShown(categoryShown: Boolean) {
        viewModelScope.launch {
            _currentCategory.value = _currentCategory.value?.withHidden(!categoryShown)
            _currentCategory.value?.let {
                categoriesUseCase.updateCategoryHidden(
                    it.categoryId,
                    it.hidden
                )
            }
        }
    }

    fun deleteCategory() {
        viewModelScope.launch {
            val category = _currentCategory.value

            if (category != null) {
                categoriesUseCase.deleteCategory(category.categoryId)
            }
            _popBackStack.update { true }
        }
    }
}
