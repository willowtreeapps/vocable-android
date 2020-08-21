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

class AddUpdateCategoryViewModel : BaseViewModel() {

    companion object {
        private const val CATEGORY_MESSAGE_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowCategoryUpdateMessage = MutableLiveData<Boolean>()
    val showCategoryUpdateMessage: LiveData<Boolean> = liveShowCategoryUpdateMessage

    private val liveCurrentCategory = MutableLiveData<Category>()
    val currentCategory: LiveData<Category> = liveCurrentCategory

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
            // Get the index of the first hidden category to find the sort order of new category
            var firstHiddenIndex = allCategories.indexOfFirst { it.hidden }
            if (firstHiddenIndex == -1) {
                firstHiddenIndex = allCategories.size - 1
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
            liveCurrentCategory.postValue(newCategory)
            delay(CATEGORY_MESSAGE_DELAY)
            liveShowCategoryUpdateMessage.postValue(false)
        }
    }
}