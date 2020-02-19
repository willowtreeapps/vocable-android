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
        "Temporal",
        "People",
        "Numbers"
    )

    private val liveCategoryList = MutableLiveData<List<String>>()
    val categoryList: LiveData<List<String>> = liveCategoryList

    private val liveSelectedCategory = MutableLiveData<String>()
    val selectedCategory: LiveData<String> = liveSelectedCategory

    init {
        liveCategoryList.postValue(categories)
    }
}