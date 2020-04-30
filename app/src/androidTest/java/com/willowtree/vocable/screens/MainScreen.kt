package com.willowtree.vocable.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.VocableButton
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf

class MainScreen {

    // Edit Text box and action buttons
    val currentText = onView(withId(R.id.current_text))
    val keyboardNavitgationButton = onView(withId(R.id.keyboard_button))
    val settingsNavigationButton = onView(withId(R.id.settings_button))

    // Categories and preset phrases
    val categoryBackButton = onView(withId(R.id.category_back_button))
    val categoryForwardButton = onView(withId(R.id.category_forward_button))

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