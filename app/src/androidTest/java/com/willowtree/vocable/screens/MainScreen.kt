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

    val defaultCategories = arrayOf("General", "Basic Needs", "Personal Care", "Conversation", "Environment", "123", "My Sayings")
    val defaultPhraseGeneral = arrayOf("Please be patient", "I don't know", "Maybe", "Yes", "I didn't mean to say that", "Please wait", "No", "Thank you")
    val defaultPhraseBasicNeeds = arrayOf("I want to sit up", "I am finished", "I am uncomfortable", "I am fine", "I want to lie down", "I am in pain", "I am good", "I am tired")

    fun verifyDefaultCategoriesExist() {
        for (category in defaultCategories) {
            onView(allOf(withText(category), withParent(withId(R.id.category_button_container))))
                .check(matches(isDisplayed()))
            categoryForwardButton.tap()
        }
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

}