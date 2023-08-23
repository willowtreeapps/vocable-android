package com.willowtree.vocable.presets.adapter

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.keyboard.KeyboardViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.CategoryPhraseCrossRef
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.util.*

class AddToCategoryPickerViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private val liveCategoryList = MutableLiveData<List<Category>>()
    val categoryList: LiveData<List<Category>> = liveCategoryList

    private val liveCategoryMap = MutableLiveData<Map<Category, Boolean>>()
    val categoryMap: LiveData<Map<Category, Boolean>> = liveCategoryMap

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    private val liveShowPhraseDeleted = MutableLiveData<Boolean>()
    val showPhraseDeleted: LiveData<Boolean> = liveShowPhraseDeleted

    private var userCategories = listOf<Category>()

    // a list that maps categories to the phrase.  The phrase will not change, so we just need
    // to map the category id to the phrase object that was added to it, so we can delete it later
    private var savedPhrases = mutableMapOf<String, Phrase>()

    fun handleCategoryToggled(phraseString: String, category: Category, isChecked: Boolean) {
        backgroundScope.launch {
            // if the user checked the switch
            if (isChecked) {
                // add it to the category
                addNewPhrase(phraseString, category)
            } else { // if they un-checked the switch
                // delete it from the category
                deletePhraseFromCategory(phraseString, category)
            }
        }
    }

    fun getCategoryList() {
        backgroundScope.launch {
            liveCategoryList.postValue(presetsRepository.getUserGeneratedCategories())
        }
    }

    fun buildCategoryList(phraseString: String, categories: List<Category>) {
        val phraseMap = mutableMapOf<Category, Boolean>()

        categories.forEach {
            phraseMap[it] = false
        }

        backgroundScope.launch {
            // map category to whether or not the phrase is in that category
            userCategories = categories

            // for each category in the list
            userCategories.forEach { category ->
                // get all the phrases associated with it
                val phrases = presetsRepository.getPhrasesForCategory(category.categoryId)

                // for each phrase in the category
                phrases.forEach {
                    // if we find a match
                    if (localizedResourceUtility.getTextFromPhrase(it) == phraseString) {
                        // add it to the map
                        phraseMap[category] = true
                    }
                }

                // if we get through the loop without adding anything, the phrase isn't in there
                if (phraseMap[category] == null) {
                    phraseMap[category] = false
                }
            }

            liveCategoryMap.postValue(phraseMap)
        }
    }

    private fun deletePhraseFromCategory(phraseString: String, category: Category) {
        backgroundScope.launch {
            var phrase: Phrase? = null

            val phrasesInCategory = presetsRepository.getPhrasesForCategory(category.categoryId)
            for (p in phrasesInCategory) {
                if (localizedResourceUtility.getTextFromPhrase(p) == phraseString) {
                    phrase = p
                }
            }

            with(presetsRepository) {
                if (phrase != null) {
                    val crossRefs = getCrossRefsForPhraseIds(listOf(phrase.phraseId))

                    // if the phrase is in only one category, delete it from the db entirely
                    if (crossRefs.size == 1) {
                        deletePhrase(phrase)
                    } else { // otherwise, just delete the one cross reference
                        crossRefs.forEach { ref ->
                            if (ref.categoryId == category.categoryId) {
                                deleteCrossRef(ref)
                            }
                        }
                    }

                    // remove the category from the saved phrases map
                    savedPhrases.remove(category.categoryId)
                }
            }

            liveShowPhraseDeleted.postValue(true)
            delay(KeyboardViewModel.PHRASE_ADDED_DELAY)
            liveShowPhraseDeleted.postValue(false)
        }
    }

    private fun addNewPhrase(phraseString: String, category: Category) {
        backgroundScope.launch {
            val categoryForPhrase = presetsRepository.getCategoryById(category.categoryId)
            val phraseId = UUID.randomUUID().toString()
            val categoryPhrases =
                presetsRepository.getPhrasesForCategory(category.categoryId)
            with(presetsRepository) {
                val phrase = Phrase(
                    phraseId,
                    System.currentTimeMillis(),
                    true,
                    System.currentTimeMillis(),
                    null,
                    mapOf(Pair(Locale.getDefault().toString(), phraseString)),
                    categoryPhrases.size
                )
                addPhrase(phrase)
                savedPhrases[category.categoryId] = phrase
                addCrossRef(
                    CategoryPhraseCrossRef(
                        categoryForPhrase.categoryId,
                        phraseId
                    )
                )
            }

            liveShowPhraseAdded.postValue(true)
            delay(KeyboardViewModel.PHRASE_ADDED_DELAY)
            liveShowPhraseAdded.postValue(false)
        }
    }

    fun getUpdatedCategoryName(category: Category): String {
        val updatedCategory = userCategories.firstOrNull { it.categoryId == category.categoryId }
        return localizedResourceUtility.getTextFromCategory(updatedCategory)
    }
}