package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.*

class AddUpdateCategoryViewModel : BaseViewModel() {

    companion object {
        private const val CATEGORY_MESSAGE_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private val liveShowCategoryUpdateMessage = MutableLiveData<Boolean>()
    val showCategoryUpdateMessage: LiveData<Boolean> = liveShowCategoryUpdateMessage

    private val liveShowDuplicateCategoryMessage = MutableLiveData<Boolean>()
    val showDuplicateCategoryMessage: LiveData<Boolean> = liveShowDuplicateCategoryMessage

    private var allCategories = listOf<Category>()

    init {
        populateAllCategories()
    }

    private fun populateAllCategories() {
        backgroundScope.launch {
            allCategories = presetsRepository.getAllCategories()
        }
    }

    fun updateCategory(categoryId: String, updatedName: String) {
        backgroundScope.launch {
            // Don't allow duplicate category names
            if (categoryNameExists(updatedName)) {
                liveShowDuplicateCategoryMessage.postValue(true)
                return@launch
            }

            val toUpdate = allCategories.firstOrNull { it.categoryId == categoryId }
            toUpdate?.let {
                val currentName = it.localizedName?.get(Locale.getDefault().toString())
                if (currentName == updatedName) {
                    return@let
                }
                val updatedNameMap = it.localizedName?.toMutableMap()?.apply {
                    put(Locale.getDefault().toString(), updatedName)
                }
                it.localizedName = updatedNameMap ?: mapOf()

                presetsRepository.updateCategory(it)

                liveShowCategoryUpdateMessage.postValue(true)
                delay(CATEGORY_MESSAGE_DELAY)
                liveShowCategoryUpdateMessage.postValue(false)
            }
        }
    }

    fun addCategory(categoryName: String) {
        backgroundScope.launch {
            // Don't allow duplicate category names
            if (categoryNameExists(categoryName)) {
                liveShowDuplicateCategoryMessage.postValue(true)
                return@launch
            }

            // Get the index of the first hidden category to find the sort order of new category
            var firstHiddenIndex = allCategories.indexOfFirst { it.hidden }
            if (firstHiddenIndex == -1) {
                firstHiddenIndex = allCategories.size
            }

            // Increase the sort order of all hidden categories since the new one will be sorted
            // before them
            val listToUpdate = allCategories.filter { it.hidden }
            listToUpdate.forEach {
                it.sortOrder++
            }

            val newCategory = Category(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                true,
                null,
                mapOf(Pair(Locale.getDefault().toString(), categoryName)),
                false,
                firstHiddenIndex
            )

            allCategories = allCategories
                .toMutableList()
                .apply { add(newCategory) }
                .sortedBy { it.sortOrder }

            with(presetsRepository) {
                addCategory(newCategory)
                updateCategories(listToUpdate)
            }

            liveShowCategoryUpdateMessage.postValue(true)
            delay(CATEGORY_MESSAGE_DELAY)
            liveShowCategoryUpdateMessage.postValue(false)
        }
    }

    private suspend fun categoryNameExists(categoryName: String): Boolean {
        val allCategories = presetsRepository.getAllCategories()
        allCategories.forEach {
            val name = localizedResourceUtility.getTextFromCategory(it)
            if (name == categoryName) {
                return true
            }
        }
        return false
    }
}