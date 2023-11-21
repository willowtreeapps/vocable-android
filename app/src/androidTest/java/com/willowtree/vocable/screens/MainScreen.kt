package com.willowtree.vocable.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.utility.tap
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

class MainScreen {

    val defaultCategories = arrayOf("General", "Basic Needs", "Personal Care", "Conversation", "Environment", "123", "Recents")
    val defaultPhraseGeneral = arrayOf("Please", "Yes", "Maybe", "I don't know", "Thank you", "No", "Please wait", "I didn't mean to say that")
    val defaultPhraseBasicNeeds = arrayOf("I need to go to the restroom", "I am hungry", "I am hot", "I am fine", "I am thirsty", "I am cold", "I am tired", "I am good")

    fun verifyDefaultCategoriesExist() {
        for (category in defaultCategories) {
            onView(allOf(withText(category), withParent(withId(R.id.category_button_container))))
                .check(matches(isDisplayed()))
            categoryForwardButton.tap()
        }
    }

    // Scrolls to the left numTimesToScroll times and then taps the current category
    fun scrollLeftAndTapCurrentCategory(numTimesToScroll: Int) {
       for (i in 1..numTimesToScroll){
            onView(withId(R.id.category_back_button)).tap()
       }
       val currentCategory = defaultCategories.size - (numTimesToScroll % defaultCategories.size)
       onView(withText(defaultCategories[currentCategory])).tap()
    }

    // Scrolls to the right numTimesToScroll times and then taps the current category
    fun scrollRightAndTapCurrentCategory(numTimesToScroll: Int) {
        for (i in 1..numTimesToScroll){
            onView(withId(R.id.category_forward_button)).tap()
        }
        val currentCategory = numTimesToScroll % defaultCategories.size
        onView(withText(defaultCategories[currentCategory])).tap()
    }

    // Verifies that the give phrase is shown on screen
    fun verifyGivenPhrasesDisplay(setOfPhrases: Array<String>) {
        for (phrase in setOfPhrases) {
            onView(withText(phrase)).check(matches(isDisplayed()))
        }
    }

    // Taps on the selected phrase
    fun tapPhrase(phraseText: String) {
        onView(withText(phraseText)).tap()
    }

    // This function verifies that we are on the Main Screen
    fun checkOnMainScreen(){
        currentText.check(matches(isDisplayed()))
    }

    // Edit Text box and action buttons
    val currentText = onView(withId(R.id.current_text))
    val keyboardNavitgationButton = onView(withId(R.id.keyboard_button))
    val settingsNavigationButton = onView(withId(R.id.settings_button))

    // Categories and preset phrases
    val categoryBackButton = onView(withId(R.id.category_back_button))
    val categoryForwardButton = onView(withId(R.id.category_forward_button))
    val selectedCategory = onView(withId(R.id.category_button_container))

    // First phrase of selected category
    val firstPhrase = onView(
            allOf(
                    instanceOf(VocableButton::class.java),
                    withParent(withId(R.id.phrases_container)),
                    withParentIndex(0)
            )
    )

    // These will appear on other pages and could be abstracted out eventually
    val phrasesBackButton = onView(withId(R.id.phrases_back_button))
    val phrasesForwardButton = onView(withId(R.id.phrases_forward_button))
    val phrasesPageNumber = onView(withId(R.id.phrases_page_number))

    // Keyboard
    val keyboardCont = onView(withId(R.id.keyboard_key_holder))

}