package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import kotlinx.android.synthetic.main.fragment_phrases.*

class PhrasesFragment : BaseFragment() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"

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
        arguments?.getStringArray(KEY_PHRASES)?.forEach { phrase ->
            val phraseButton =
                inflater.inflate(R.layout.phrase_button, phrasesContainer, false) as VocableButton
            phraseButton.text = phrase
            phrasesContainer?.addView(phraseButton)
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