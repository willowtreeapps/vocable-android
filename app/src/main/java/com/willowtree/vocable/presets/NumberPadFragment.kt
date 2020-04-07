package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateMargins
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.databinding.FragmentNumberPadBinding
import com.willowtree.vocable.databinding.PhraseButtonBinding
import com.willowtree.vocable.room.Phrase
import java.util.*
import kotlin.collections.ArrayList

class NumberPadFragment : BaseFragment() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"
        const val MAX_PHRASES = 12

        fun newInstance(phrases: List<Phrase>) = NumberPadFragment().apply {
            arguments = bundleOf(KEY_PHRASES to ArrayList(phrases))
        }
    }

    private var numColumns = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentNumberPadBinding.inflate(inflater, container, false)

        numColumns = resources.getInteger(R.integer.number_pad_columns)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseButton =
                PhraseButtonBinding.inflate(inflater, binding?.phrasesContainer, false)
            with(phraseButton.root as VocableButton) {
                val pair = phrase.getLocalizedPair()
                setText(pair.first, Locale.getDefault())
                // Remove end margin on last column
                if (index % numColumns == numColumns - 1) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        marginEnd = 0
                    }
                }
                if (index >= MAX_PHRASES - numColumns) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        updateMargins(bottom = 0)
                    }
                }
            }
            binding?.phrasesContainer?.addView(phraseButton.root)
        }

        return binding?.root
    }

    override fun getAllViews() = emptyList<View>()
}