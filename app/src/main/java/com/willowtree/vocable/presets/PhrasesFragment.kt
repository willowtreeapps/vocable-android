package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.databinding.FragmentPhrasesBinding
import com.willowtree.vocable.databinding.PhraseButtonBinding

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

    private var binding: FragmentPhrasesBinding? = null
    private val allViews = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhrasesBinding.inflate(inflater, container, false)

        val phrases = arguments?.getStringArray(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseButton =
                PhraseButtonBinding.inflate(inflater, binding?.phrasesContainer, false)
            with(phraseButton.root as VocableButton) {
                text = phrase
                // Remove end margin on last column
                if (index % NUM_COLUMNS == NUM_COLUMNS - 1) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        marginEnd = 0
                    }
                }
            }
            binding?.phrasesContainer?.addView(phraseButton.root)
        }
        phrases?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until PresetsFragment.MAX_PHRASES - it.size) {
                val hiddenButton =
                    PhraseButtonBinding.inflate(inflater, binding?.phrasesContainer, false)
                binding?.phrasesContainer?.addView(hiddenButton.root.apply {
                    isEnabled = false
                    visibility = View.INVISIBLE
                })
            }
        }

        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.phrasesContainer)
        }
        return allViews
    }

    private fun getAllChildViews(viewGroup: ViewGroup?) {
        viewGroup?.children?.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }
}