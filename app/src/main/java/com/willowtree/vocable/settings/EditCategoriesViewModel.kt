package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject

class EditCategoriesViewModel(numbersCategoryId: String, mySayingsCategoryId: String) :
    BaseViewModel(numbersCategoryId, mySayingsCategoryId) {

    companion object {
        private const val CATEGORY_UPDATED_DELAY = 2000L
        private const val CATEGORY_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveCategoryList = MutableLiveData<List<Category>>()
    val categoryList: LiveData<List<Category>> = liveCategoryList

    private val liveCategoryUserGenerated = MutableLiveData<Boolean>()
    val categoryIsUserGenerated: LiveData<Boolean> = liveCategoryUserGenerated

    private val liveSetButtonsEnabled = MutableLiveData<Boolean>()
    val setButtonEnabled: LiveData<Boolean> = liveSetButtonsEnabled

    private val liveShowCategoryAdded = MutableLiveData<Boolean>()
    val showCategoryAdded: LiveData<Boolean> = liveShowCategoryAdded

    init {
        populateCategories()
    }

    private fun populateCategories() {
        backgroundScope.launch {

            val categories = presetsRepository.getAllCategories()

            liveCategoryList.postValue(categories)
        }
    }

    fun deleteCategory(category: Category) {
        backgroundScope.launch {
            presetsRepository.deleteCategory(category)
            populateCategories()
        }
    }

    fun setEditButtonsEnabled(enabled: Boolean) {
        liveSetButtonsEnabled.postValue(enabled)
    }

    fun updateCategory(category: Category) {
        backgroundScope.launch {
            presetsRepository.updateCategory(category)
            populateCategories()

            liveShowCategoryAdded.postValue(true)
            delay(CATEGORY_UPDATED_DELAY)
            liveShowCategoryAdded.postValue(false)
        }
    }

//    fun addNewCategory(categoryStr: String) {
//        backgroundScope.launch {
//            val categoryId = presetsRepository.getCategoryId(categoryStr)
//            presetsRepository.addCategory(
//                Category(
//                    System.currentTimeMillis(),
//                    System.currentTimeMillis(),
//                    true,
//                    categoryStr
//                )
//            )
//
//            populateCategories()
//
//            liveShowCategoryAdded.postValue(true)
//            delay(CATEGORY_ADDED_DELAY)
//            liveShowCategoryAdded.postValue(false)
//        }
//    }

}
