package com.willowtree.vocable.presets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PresetsViewModel : ViewModel() {

    private val categories = listOf(
        "General",
        "Basic Needs",
        "Personal Care",
        "Conversation",
        "Environment",
        "Feelings",
        "Questions",
        "Time",
        "People",
        "Numbers"
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
        "Please to meet you",
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

    private val liveCategoryList = MutableLiveData<List<String>>()
    val categoryList: LiveData<List<String>> = liveCategoryList

    private val liveSelectedCategory = MutableLiveData<String>()
    val selectedCategory: LiveData<String> = liveSelectedCategory

    init {
        liveCategoryList.postValue(categories)
    }

    fun onCategorySelected(category: String) {
        liveSelectedCategory.postValue(category)
    }
}