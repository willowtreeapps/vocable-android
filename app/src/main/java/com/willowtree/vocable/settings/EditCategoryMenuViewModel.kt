package com.willowtree.vocable.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class EditCategoryMenuViewModel: BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private var overallCategories = listOf<Category>()

    private val _currentCategory = MutableLiveData<Category>()
    val currentCategory: LiveData<Category> = _currentCategory

    private val _showCategoryStatus = MutableLiveData<Boolean>()
    val showCategoryStatus: LiveData<Boolean> = _showCategoryStatus


    fun updateCategoryById(categoryId: String) {
        backgroundScope.launch{
            _currentCategory.postValue(presetsRepository.getCategoryById(categoryId))
            if(_currentCategory.value?.hidden==null){
                _showCategoryStatus.postValue(true)
            }else{
                _showCategoryStatus.postValue(!_currentCategory.value?.hidden!!)
            }
        }

    }

    fun updateHiddenStatus(showCategoryStatus: Boolean) {
        backgroundScope.launch {
            _currentCategory.value?.hidden = !showCategoryStatus
            _currentCategory?.value?.let { presetsRepository.updateCategory(_currentCategory.value!!) }
            _currentCategory.value?.categoryId?.let { updateCategoryById(it) }
        }
    }

    fun updateCurrentCategory(category:Category){
        _currentCategory.value = category
    }
    fun getUpdatedCategoryName(category: Category): String {
        return localizedResourceUtility.getTextFromCategory(category)
    }

    fun updateNewCategoryFromRepository(){
        backgroundScope.launch{
            _currentCategory.value?.let { presetsRepository.getCategoryById(it.categoryId) }
        }
    }

}