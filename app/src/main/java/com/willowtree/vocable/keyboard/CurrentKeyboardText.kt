package com.willowtree.vocable.keyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object CurrentKeyboardText {

    private val liveTypedText = MutableLiveData<String>()
    val typedText: LiveData<String> = liveTypedText

    fun clearTypedText() {
        liveTypedText.postValue(null)
    }

    fun addCharacterToText(character: String) {
        val newText = if (liveTypedText.value == null) {
            character
        } else {
            liveTypedText.value.plus(character)
        }
        liveTypedText.postValue(newText)
    }

    fun backspaceCharacter() {
        val newText = if (liveTypedText.value == null) {
            return
        } else {
            liveTypedText.value.toString().dropLast(1)
        }
        liveTypedText.postValue(newText)
    }

    fun spaceCharacter() {
        val newText = if (liveTypedText.value == null) {
            return
        } else {
            liveTypedText.value.plus(" ")
        }
        liveTypedText.postValue(newText)
    }
}