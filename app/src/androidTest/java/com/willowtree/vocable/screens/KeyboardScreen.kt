package com.willowtree.vocable.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.willowtree.vocable.R
import com.willowtree.vocable.utility.tap

class KeyboardScreen {

    // Keyboard
    val keyboardInputText = onView(withId(R.id.keyboard_input))
    val keyboardCont = onView(withId(R.id.keyboard_key_holder))
    val keyboardSpaceButton = onView(withId(R.id.keyboard_space_button))
    val keyboardBackspaceButton = onView(withId(R.id.keyboard_backspace_button))
    val keyboardClearButton = onView(withId(R.id.keyboard_clear_button))
    val keyboardPresetsButton = onView(withId(R.id.presets_button))

    fun tapSeveralLetters(letters: String) {
        val numLetters = letters.length

        for (i in 1..numLetters) {
            try {
                if(letters[i-1] == ' ')
                    keyboardSpaceButton.tap()
                else
                    onView(withText(letters[i - 1].toString().toUpperCase())).tap()
            } catch (e: NoMatchingViewException) {
                //do nothing if the character isn't one we support
            }
        }
    }
}