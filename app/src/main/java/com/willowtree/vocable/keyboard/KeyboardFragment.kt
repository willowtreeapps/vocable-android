package com.willowtree.vocable.keyboard

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.fragment_keyboard.*

class KeyboardFragment : BaseFragment() {

    companion object {
        private const val TRASH = "TRASH"
        private const val BACKSPACE = "BACKSPACE"
        private const val SPACE = "SPACE"
        private const val SPEAK = "SPEAK"

        private val KEYS = listOf(
            "Q",
            "W",
            "E",
            "R",
            "T",
            "Y",
            "U",
            "I",
            "O",
            "P",
            "A",
            "S",
            "D",
            "F",
            "G",
            "H",
            "J",
            "K",
            "L",
            TRASH,
            BACKSPACE,
            "Z",
            "X",
            "C",
            "V",
            "B",
            "N",
            "M",
            SPACE,
            SPEAK
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        view?.let {
            it.findViewById<View>(R.id.predictive_text)?.isVisible = false
            populateKeys(it)
        }

        return view
    }

    private fun populateKeys(baseView: View) {
        val gridLayout = baseView.findViewById<GridLayout>(R.id.keyboard_key_holder)

        KEYS.withIndex().forEach {
            when (it.value) {
                SPACE, SPEAK, TRASH, BACKSPACE -> {
                    layoutInflater.inflate(R.layout.keyboard_action_layout, gridLayout, true)
                }
                else -> {
                    layoutInflater.inflate(R.layout.keyboard_key_layout, gridLayout, true)
                }
            }
            val key = (gridLayout?.getChildAt(it.index) as VocableButton).apply {
                text = it.value
            }
            when (it.value) {
                SPACE -> {
                    with(key as ActionButton) {
                        text = null
                        setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_space_bar_56dp,
                            0, 0, 0
                        )
                        action = {
                            CurrentKeyboardText.spaceCharacter()
                        }
                    }
                }
                SPEAK -> {
                    with(key as ActionButton) {
                        text = null
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_speak_56dp, 0, 0, 0)
                        action = {
                            VocableTextToSpeech.getTextToSpeech()?.speak(
                                keyboard_input.text,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                id.toString()
                            )
                        }
                    }
                }
                TRASH -> {
                    with(key as ActionButton) {
                        text = null
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete_56dp, 0, 0, 0)
                        action = {
                            CurrentKeyboardText.clearTypedText()
                        }
                    }
                }
                BACKSPACE -> {
                    with(key as ActionButton) {
                        text = null
                        setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_backpace_56dp, 0, 0, 0
                        )
                        action = {
                            CurrentKeyboardText.backspaceCharacter()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CurrentKeyboardText.typedText.observe(viewLifecycleOwner, Observer {
            keyboard_input.setText(it ?: getString(R.string.keyboard_select_letters))
        })
    }

    override fun onDestroy() {
        CurrentKeyboardText.clearTypedText()
        super.onDestroy()
    }

    private val allViews = mutableListOf<View>()

    override fun getLayout(): Int = R.layout.fragment_keyboard

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(keyboard_parent)
        }
        return allViews
    }

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }
}