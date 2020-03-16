package com.willowtree.vocable.presets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.launch
import org.koin.core.inject

class PresetsViewModel : BaseViewModel() {

    companion object {
        private const val CATEGORY_GENERAL = "General"
        private const val CATEGORY_BASIC_NEEDS = "Basic Needs"
        private const val CATEGORY_PERSONAL_CARE = "Personal Care"
        private const val CATEGORY_CONVERSATION = "Conversation"
        private const val CATEGORY_ENVIRONMENT = "Environment"
        private const val CATEGORY_FEELINGS = "Feelings"
        private const val CATEGORY_QUESTIONS = "Questions"
        private const val CATEGORY_TIME = "Time"
        private const val CATEGORY_PEOPLE = "People"
        private const val CATEGORY_NUMBERS = "Numbers"
        private const val CATEGORY_MY_SAYINGS = "My Sayings"
    }

    private val presetsRepository: PresetsRepository by inject()

    private val categories = mutableListOf(
        CATEGORY_GENERAL,
        CATEGORY_BASIC_NEEDS,
        CATEGORY_PERSONAL_CARE,
        CATEGORY_CONVERSATION,
        CATEGORY_ENVIRONMENT,
        CATEGORY_FEELINGS,
        CATEGORY_QUESTIONS,
        CATEGORY_TIME,
        CATEGORY_PEOPLE,
        CATEGORY_NUMBERS,
        CATEGORY_MY_SAYINGS
    )

    private val generalPhrases = listOf(
        "Please",
        "Thank you",
        "Yes",
        "No",
        "Maybe",
        "Please wait",
        "I don't know",
        "I didn't mean to say that",
        "Please be patient",
        "Please ask me a yes/no question",
        "Please edit a custom phrase for me.",
        "Please delete a custom phrase for me."
    )

    private val basicNeedsPhrases = listOf(
        "I need to go to the restroom",
        "I am thirsty",
        "I am hungry",
        "I am cold",
        "I am hot",
        "I am tired",
        "I am fine",
        "I am good",
        "I am uncomfortable",
        "I am in pain",
        "I am finished",
        "I want to lie down",
        "I want to sit up"
    )

    private val personalCarePhrases = listOf(
        "I need my medication",
        "I need a bath",
        "I need a shower",
        "I need to wash my face",
        "I need to change my clothes",
        "I need to brush my teeth",
        "I need to brush my hair",
        "Please check my...",
        "Please stretch my...",
        "Please fix my pillow",
        "I need to spit",
        "I am having trouble breathing"
    )

    private val conversationPhrases = listOf(
        "Hello",
        "Good morning",
        "Good evening",
        "Pleased to meet you",
        "How is your day?",
        "How are you?",
        "How's it going?",
        "How was your weekend?",
        "Goodbye",
        "Okay",
        "Bad",
        "Good",
        "That makes sense",
        "I like it",
        "Please stop",
        "I do not agree",
        "Please repeat what you said"
    )

    private val environmentPhrases = listOf(
        "Please turn the lights on",
        "Please turn the lights off",
        "No visitors please",
        "I would like visitors",
        "Please be quiet",
        "I would like to talk",
        "Please turn the TV on",
        "Please turn the TV off",
        "Please turn the volume up",
        "Please turn the volume down",
        "Please open the blinds",
        "Please close the blinds",
        "Please open the window",
        "Please close the window"
    )

    private val feelingsPhrases = listOf(
        "I am nauseous",
        "I am frustrated",
        "I am happy",
        "I am sad",
        "I am worried",
        "I am mad",
        "I am afraid",
        "I am dizzy",
        "I am tired",
        "I am awake",
        "I am light-headed",
        "I am disappointed",
        "I am anxious",
        "I am cold",
        "I am hot"
    )

    private val questionsPhrases = listOf(
        "What day is it?",
        "What time is it?",
        "When are you coming back?",
        "How are you?",
        "When are you leaving?",
        "How was your day?",
        "What medication is that?",
        "What are you holding?",
        "Why?",
        "Who?"
    )

    private val timePhrases = listOf(
        "Morning",
        "Afternoon",
        "Night",
        "Right now",
        "Recently",
        "A few minutes ago",
        "A few hours ago",
        "Today",
        "Yesterday",
        "Tomorrow",
        "This week",
        "Last week",
        "Next week",
        "This month",
        "Last month",
        "Next month",
        "A few days ago",
        "A few months ago",
        "A few years ago",
        "In a few days",
        "In a few months",
        "In a few years"
    )

    private val peoplePhrases = listOf(
        "Doctor",
        "Nurse",
        "Me",
        "You",
        "Them",
        "Mom",
        "Dad",
        "Sister",
        "Brother",
        "Cousin",
        "Partner",
        "Husband",
        "Wife",
        "Son",
        "Daughter",
        "Children",
        "Family",
        "In-law"
    )

    private val numbersPhrases = listOf(
        "0",
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "Yes",
        "No",
        "Unsure"
    )

    private val categoriesMap = mutableMapOf(
        Pair(CATEGORY_GENERAL, generalPhrases),
        Pair(CATEGORY_BASIC_NEEDS, basicNeedsPhrases),
        Pair(CATEGORY_PERSONAL_CARE, personalCarePhrases),
        Pair(CATEGORY_CONVERSATION, conversationPhrases),
        Pair(CATEGORY_ENVIRONMENT, environmentPhrases),
        Pair(CATEGORY_FEELINGS, feelingsPhrases),
        Pair(CATEGORY_QUESTIONS, questionsPhrases),
        Pair(CATEGORY_TIME, timePhrases),
        Pair(CATEGORY_PEOPLE, peoplePhrases),
        Pair(CATEGORY_NUMBERS, numbersPhrases)
    )

    private val liveCategoryList = MutableLiveData<List<Category>>()
    val categoryList: LiveData<List<Category>> = liveCategoryList

    private val liveSelectedCategory = MutableLiveData<Category>()
    val selectedCategory: LiveData<Category> = liveSelectedCategory

    private val liveCurrentPhrases = MutableLiveData<List<Phrase>>()
    val currentPhrases: LiveData<List<Phrase>> = liveCurrentPhrases

    init {
        populateCategories()
    }

    fun populateCategories() {
        backgroundScope.launch {
            val categories = presetsRepository.getAllCategories().toMutableList()
            if (categories.isEmpty()) {
                populateDatabase()
            } else {
                if (presetsRepository.getUserGeneratedPhrases().isEmpty()) {
                    categories.removeIf {
                        it.name == CATEGORY_MY_SAYINGS
                    }
                }
                val currentCategoryList = liveCategoryList.value
                val currentCategory = liveSelectedCategory.value
                if (currentCategoryList?.size != categories.size || !currentCategoryList.containsAll(
                        categories
                    )
                ) {
                    liveCategoryList.postValue(categories)
                }
                if (currentCategory == null || !categories.contains(currentCategory)) {
                    onCategorySelected(categories.first())
                } else {
                    // Update phrases for category if needed
                    onCategorySelected(currentCategory)
                }
            }
        }
    }

    // This is a temporary solution for populating the database until we set up JSON sources
    private fun populateDatabase() {
        backgroundScope.launch {
            val categoryObjects = mutableListOf<Category>()
            categories.forEachIndexed { index, name ->
                categoryObjects.add(
                    Category(
                        index.toLong() + 1,
                        System.currentTimeMillis(),
                        false,
                        name
                    )
                )
            }
            presetsRepository.populateCategories(categoryObjects)

            var phraseIndex = 1L
            val phraseObjects = mutableListOf<Phrase>()
            categoryObjects.forEach { category ->
                val phraseStrings = categoriesMap[category.name]
                phraseStrings?.forEach { phraseString ->
                    phraseObjects.add(
                        Phrase(
                            phraseIndex,
                            System.currentTimeMillis(),
                            false,
                            0L,
                            phraseString,
                            category.identifier
                        )
                    )
                    phraseIndex++
                }
            }
            presetsRepository.populatePhrases(phraseObjects)

            liveCategoryList.postValue(categoryObjects)
            onCategorySelected(categoryObjects.first())
        }
    }

    fun onCategorySelected(category: Category) {
        liveSelectedCategory.postValue(category)
        backgroundScope.launch {
            val phrases = if (category.name == CATEGORY_MY_SAYINGS) {
                presetsRepository.getUserGeneratedPhrases()
            } else {
                presetsRepository.getPhrasesForCategory(category.identifier)
            }
            liveCurrentPhrases.postValue(phrases)
        }
    }
}