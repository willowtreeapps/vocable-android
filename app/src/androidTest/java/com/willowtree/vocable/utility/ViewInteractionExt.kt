package com.willowtree.vocable.utility

import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers

fun ViewInteraction.tap() {
    check(ViewAssertions.matches(ViewMatchers.isDisplayed())) // Ensure view is displayed before tapping
    perform(ViewActions.click())
}

fun ViewInteraction.assertTextMatches(text: String) {
    check(ViewAssertions.matches(ViewMatchers.isDisplayed())) // Ensure view is displayed before checking text
    check(ViewAssertions.matches(ViewMatchers.withText(CoreMatchers.containsString(text))))
}

fun ViewInteraction.assertElementExists() {
    check(ViewAssertions.matches(CoreMatchers.not(ViewAssertions.doesNotExist())))
}

// Remove waitForDisplayed since Espresso's IdlingResource handles the waiting
// The IdlingResourceTestRule in BaseTest will ensure proper synchronization