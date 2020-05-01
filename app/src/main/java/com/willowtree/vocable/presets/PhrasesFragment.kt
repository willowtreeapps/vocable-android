package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.updateMargins
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableButton
import com.willowtree.vocable.databinding.FragmentPhrasesBinding
import com.willowtree.vocable.databinding.PhraseButtonBinding
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocaleUtils
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.collections.ArrayList

class PhrasesFragment : BaseFragment<FragmentPhrasesBinding>() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"

        fun newInstance(phrases: List<Phrase>): PhrasesFragment {
            return PhrasesFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_PHRASES, ArrayList(phrases))
                }
            }
        }
    }

    override val bindingInflater: BindingInflater<FragmentPhrasesBinding> =
        FragmentPhrasesBinding::inflate
    private val allViews = mutableListOf<View>()
    private var maxPhrases = 1
    private var numColumns = 1
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        maxPhrases = resources.getInteger(R.integer.max_phrases)
        numColumns = resources.getInteger(R.integer.phrases_columns)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseButton =
                PhraseButtonBinding.inflate(inflater, binding.phrasesContainer, false)
            with(phraseButton.root as VocableButton) {
                val (phraseStr, locale) = localizedResourceUtility.getValueLocalePairFromPhrase(phrase) ?: Pair("", Locale.getDefault())
                setText(phraseStr, locale)
                // Remove end margin on last column
                if (index % numColumns == numColumns - 1) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        marginEnd = 0
                    }
                }
                if (index >= maxPhrases - numColumns) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        updateMargins(bottom = 0)
                    }
                }
            }
            binding.phrasesContainer.addView(phraseButton.root)
        }
        phrases?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until maxPhrases - it.size) {
                val hiddenButton =
                    PhraseButtonBinding.inflate(inflater, binding.phrasesContainer, false)
                binding.phrasesContainer.addView(hiddenButton.root.apply {
                    isEnabled = false
                    visibility = View.INVISIBLE
                })
            }
        }

        return binding.root
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.phrasesContainer)
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