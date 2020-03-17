package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.updateMargins
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentEditPhrasesBinding
import com.willowtree.vocable.databinding.PhraseEditLayoutBinding
import com.willowtree.vocable.room.Phrase
import kotlinx.android.synthetic.main.phrase_edit_layout.view.*

class EditPhrasesFragment: BaseFragment() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"

        fun newInstance(phrases: List<Phrase>): EditPhrasesFragment {
            return EditPhrasesFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY_PHRASES, ArrayList(phrases))
                }
            }
        }
    }

    private var binding: FragmentEditPhrasesBinding? = null
    private lateinit var editPhrasesViewModel: EditPhrasesViewModel
    private val allViews = mutableListOf<View>()
    private var maxPhrases = 1
    private var numColumns = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditPhrasesBinding.inflate(inflater, container, false)

        maxPhrases = resources.getInteger(R.integer.max_edit_phrases)
        numColumns = resources.getInteger(R.integer.edit_phrases_columns)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseView = PhraseEditLayoutBinding.inflate(inflater, binding?.editPhrasesContainer, false)
            with(phraseView.root) {
                phrase_edit_text.text = phrase.utterance
                phrase_edit_text.tag = phrase
                // Remove end margin on last column
                if (index % numColumns == numColumns - 1) {
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        marginEnd = 0
                    }
                }
                if(index >= maxPhrases - numColumns){
                    layoutParams = (layoutParams as GridLayout.LayoutParams).apply {
                        updateMargins(bottom = 0)
                    }
                }
            }

            phraseView.actionButtonContainer.deleteSayingsButton.action = {
                if (phraseView.phraseEditText.tag is Phrase) {
                    editPhrasesViewModel.deletePhrase(phraseView.phraseEditText.tag as Phrase)
                }
            }

            phraseView.actionButtonContainer.editSayingsButton.action = {
                parentFragmentManager
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.settings_fragment_container, EditKeyboardFragment.newInstance(phrase))
                    .commit()
            }

            binding?.editPhrasesContainer?.addView(phraseView.root)
        }

        phrases?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until maxPhrases - it.size) {
                val hiddenView =
                    PhraseEditLayoutBinding.inflate(inflater, binding?.editPhrasesContainer, false)
                binding?.editPhrasesContainer?.addView(hiddenView.root.apply {
                    isEnabled = false
                    isInvisible = true
                })
            }
        }

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editPhrasesViewModel = ViewModelProviders.of(requireActivity()).get(EditPhrasesViewModel::class.java)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.editPhrasesContainer)
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