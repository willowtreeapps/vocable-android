package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.inject
import java.util.*

class EditCategoriesViewModel : BaseViewModel() {

    companion object {
        private const val CATEGORY_UPDATED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveCategoryList = MutableLiveData<List<Category>>()
    val categoryList: LiveData<List<Category>> = liveCategoryList

    private val liveOrderCategoryList = MutableLiveData<List<Category>>()
    val orderCategoryList: LiveData<List<Category>> = liveOrderCategoryList

    private val liveAddRemoveCategoryList = MutableLiveData<List<Category>>()
    val addRemoveCategoryList: LiveData<List<Category>> = liveAddRemoveCategoryList

    private val liveShowCategoryAdded = MutableLiveData<Boolean>()
    val showCategoryAdded: LiveData<Boolean> = liveShowCategoryAdded

    private val liveLastViewedIndex = MutableLiveData<Int>()
    val lastViewedIndex: LiveData<Int> = liveLastViewedIndex

    private var overallCategories = listOf<Category>()

    init {
        populateCategories()
    }

    private fun populateCategories() {
        backgroundScope.launch {

            overallCategories = presetsRepository.getAllCategories()

            liveOrderCategoryList.postValue(overallCategories)
            liveAddRemoveCategoryList.postValue(overallCategories)
        }
    }

    fun deleteCategory(category: Category) {
        backgroundScope.launch {
            presetsRepository.deleteCategory(category)
            populateCategories()
        }
    }

    fun updateCategory(category: Category) {
        backgroundScope.launch {
            presetsRepository.updateCategory(category)

            liveShowCategoryAdded.postValue(true)
            delay(CATEGORY_UPDATED_DELAY)
            liveShowCategoryAdded.postValue(false)
        }
    }

    fun onCategorySelected(category: Category) {
        val index = overallCategories.indexOf(category)
        if (index > -1) {
            liveLastViewedIndex.postValue(index)
        }
    }

    fun hideShowCategory(category: Category, hide: Boolean) {
        backgroundScope.launch {
            if (hide) {
                hideCategory(category)
            } else {
                showCategory(category)
            }
        }
    }

    private suspend fun hideCategory(category: Category) {
        val catIndex = overallCategories.indexOf(category)
        if (catIndex > -1) {
            val listToUpdate = overallCategories.filter { it.sortOrder >= category.sortOrder }
            listToUpdate.forEach {
                if (it.categoryId == category.categoryId) {
                    it.sortOrder = overallCategories.size - 1
                    it.hidden = true
                } else {
                    it.sortOrder--
                }
            }

            overallCategories = overallCategories.sortedBy { it.sortOrder }
            liveOrderCategoryList.postValue(overallCategories)

            presetsRepository.updateCategories(listToUpdate)
        }
    }

    private suspend fun showCategory(category: Category) {
        val catIndex = overallCategories.indexOf(category)
        if (catIndex > -1) {
            var firstHiddenIndex = overallCategories.indexOfFirst { it.hidden }
            if (firstHiddenIndex == -1) {
                firstHiddenIndex = overallCategories.size - 1
            }
            val listToUpdate = overallCategories.filter { it.hidden }
            listToUpdate.forEach {
                if (it.categoryId == category.categoryId) {
                    it.sortOrder = firstHiddenIndex
                    it.hidden = false
                } else {
                    it.sortOrder++
                }
            }

            overallCategories = overallCategories.sortedBy { it.sortOrder }
            liveOrderCategoryList.postValue(overallCategories)

            presetsRepository.updateCategories(listToUpdate)
        }
    }

    fun moveCategoryUp(category: Category) {
        backgroundScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > 0) {
                val previousCat = overallCategories[catIndex - 1]
                category.sortOrder--
                previousCat.sortOrder++

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                presetsRepository.updateCategories(listOf(category, previousCat))
            }
        }
    }

    fun moveCategoryDown(category: Category) {
        backgroundScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > -1) {
                val nextCat = overallCategories[catIndex + 1]
                category.sortOrder++
                nextCat.sortOrder--

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                presetsRepository.updateCategories(listOf(category, nextCat))
            }
        }
    }

    fun addNewCategory(categoryStr: String) {
        backgroundScope.launch {
            var firstHiddenIndex = overallCategories.indexOfFirst { it.hidden }
            if (firstHiddenIndex == -1) {
                firstHiddenIndex = overallCategories.size - 1
            }
            val listToUpdate = overallCategories.filter { it.hidden }
            listToUpdate.forEach {
                it.sortOrder++
            }
            val newCategory = Category(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                true,
                null,
                mapOf(Pair(Locale.getDefault().toString(), categoryStr)),
                false,
                firstHiddenIndex
            )

            overallCategories = overallCategories
                .toMutableList()
                .apply { add(newCategory) }
                .sortedBy { it.sortOrder }
            liveAddRemoveCategoryList.postValue(overallCategories)
            liveLastViewedIndex.postValue(firstHiddenIndex)
            liveOrderCategoryList.postValue(overallCategories)

            with(presetsRepository) {
                addCategory(newCategory)
                updateCategories(listToUpdate)
            }
        }
    }

}
