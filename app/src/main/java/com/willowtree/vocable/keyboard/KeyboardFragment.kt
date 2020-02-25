package com.willowtree.vocable.keyboard

import android.content.Intent
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
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.presets.PresetsFragment
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.android.synthetic.main.fragment_keyboard.*
import kotlinx.android.synthetic.main.keyboard_action_buttons.*


class KeyboardFragment : BaseFragment() {

    companion object {
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
            "'",
            "Z",
            "X",
            "C",
            "V",
            "B",
            "N",
            "M",
            ",",
            ".",
            "?"
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
            layoutInflater.inflate(R.layout.keyboard_key_layout, gridLayout, true)
            (gridLayout?.getChildAt(it.index) as VocableButton).text = it.value
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CurrentKeyboardText.typedText.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                keyboard_input.setText(R.string.keyboard_select_letters)
            } else {
                keyboard_input.setText(it)
            }
        })
        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            speaker_icon.isVisible = it ?: false
        })

        presets_button.action = {
            fragmentManager
                ?.beginTransaction()
                ?.replace(R.id.fragment_container, PresetsFragment())
                ?.commit()
        }

        settings_button.action = {
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        keyboard_clear_button.action = {
            CurrentKeyboardText.clearTypedText()
        }

        keyboard_space_button.action = {
            CurrentKeyboardText.spaceCharacter()
        }

        keyboard_backspace_button.action = {
            CurrentKeyboardText.backspaceCharacter()
        }

        keyboard_speak_button.action = {
            VocableTextToSpeech.getTextToSpeech()?.speak(
                keyboard_input.text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                id.toString()
            )
        }
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