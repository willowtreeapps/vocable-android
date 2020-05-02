package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentEditPhrasesBinding
import com.willowtree.vocable.databinding.PhraseEditLayoutBinding
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject

class EditPhrasesFragment : BaseFragment<FragmentEditPhrasesBinding>() {

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

    override val bindingInflater: BindingInflater<FragmentEditPhrasesBinding> = FragmentEditPhrasesBinding::inflate
    private lateinit var editPhrasesViewModel: EditPhrasesViewModel
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
        maxPhrases = resources.getInteger(R.integer.max_edit_phrases)
        numColumns = resources.getInteger(R.integer.edit_phrases_columns)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)
        phrases?.forEachIndexed { index, phrase ->
            val phraseView =
                PhraseEditLayoutBinding.inflate(inflater, binding.editPhrasesContainer, false)
            with(phraseView) {
                phraseEditText.text = localizedResourceUtility.getTextFromPhrase(phrase)
                phraseEditText.tag = phrase
            }
            with(phraseView.root) {
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

            phraseView.actionButtonContainer.deleteSayingsButton.action = {
                showDeletePhraseDialog(phrase)
            }

            phraseView.actionButtonContainer.editSayingsButton.action = {
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(
                        R.id.settings_fragment_container,
                        EditKeyboardFragment.newInstance(phrase)
                    )
                    .commit()
            }

            binding.editPhrasesContainer.addView(phraseView.root)
        }

        phrases?.let {
            // Add invisible views to fill out the rest of the space
            for (i in 0 until maxPhrases - it.size) {
                val hiddenView =
                    PhraseEditLayoutBinding.inflate(inflater, binding.editPhrasesContainer, false)
                binding.editPhrasesContainer.addView(hiddenView.root.apply {
                    isEnabled = false
                    isInvisible = true
                })
            }
        }

        return binding.root
    }

    private fun showDeletePhraseDialog(phrase: Phrase) {
        setSettingsButtonsEnabled(false)

        binding.deleteConfirmation.apply {
            dialogTitle.text = getString(R.string.are_you_sure)
            dialogMessage.text = getString(R.string.delete_warning)
            dialogPositiveButton.apply {
                text = getString(R.string.delete)
                action = {
                    editPhrasesViewModel.deletePhrase(phrase)
                    toggleDialogVisibility(false)
                    setSettingsButtonsEnabled(true)
                }
            }
            dialogNegativeButton.apply {
                text = getString(R.string.settings_dialog_cancel)
                action = {
                    toggleDialogVisibility(false)
                    setSettingsButtonsEnabled(true)
                }
            }
        }

        toggleDialogVisibility(true)
    }

    private fun setSettingsButtonsEnabled(enable: Boolean) {
        editPhrasesViewModel.setEditButtonsEnabled(enable)

    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.deleteConfirmation.root.isVisible = visible
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editPhrasesViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory()
            ).get(EditPhrasesViewModel::class.java)
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.editPhrasesContainer)
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