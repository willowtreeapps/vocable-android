package com.willowtree.vocable.utility

import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers

fun ViewInteraction.tap() {
    perform(ViewActions.click())
}

fun ViewInteraction.assertTextMatches(text: String) {
    check(ViewAssertions.matches(ViewMatchers.withText(CoreMatchers.containsString(text))))
}

fun ViewInteraction.assertElementExists() {
    check(ViewAssertions.matches(CoreMatchers.not(ViewAssertions.doesNotExist())))
}