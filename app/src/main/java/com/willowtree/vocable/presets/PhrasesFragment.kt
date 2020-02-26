package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.ActionButton
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.keyboard.KeyboardFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_phrases.*

class PhrasesFragment : BaseFragment() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"
        private const val NUM_COLUMNS = 3

        fun newInstance(phrases: List<String>): PhrasesFragment {
            return PhrasesFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(KEY_PHRASES, phrases.toTypedArray())
                }
            }
        }
    }

    private val allViews = mutableListOf<View>()

    override fun getLayout(): Int = R.layout.fragment_phrases

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val phrasesContainer = view?.findViewById<GridLayout>(R.id.phrases_container)
        val phrases = arguments?.getStringArray(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseButton =
                inflater.inflate(R.layout.phrase_button, phrasesContainer, false) as VocableButton
            with(phraseButton) {
                text = phrase
                // Remove end margin on last column
                if (index % NUM_COLUMNS == NUM_COLUMNS - 1) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        marginEnd = 0
                    }
                }
            }
            phraseButton.text = phrase
            phrasesContainer?.addView(phraseButton)
        }
        phrases?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until PresetsFragment.MAX_PHRASES - it.size) {
                val hiddenButton =
                    inflater.inflate(R.layout.phrase_button, phrasesContainer, false).apply {
                        isEnabled = false
                        visibility = View.INVISIBLE
                    }
                phrasesContainer?.addView(hiddenButton)
            }
        }

        return view
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(phrases_container)
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