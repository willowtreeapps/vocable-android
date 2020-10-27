package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentPhrasesBinding
import com.willowtree.vocable.presets.adapter.PhraseAdapter
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.ItemOffsetDecoration
import org.koin.android.viewmodel.ext.android.viewModel

class PhrasesFragment : BaseFragment<FragmentPhrasesBinding>() {
    private val presetsViewModel: PresetsViewModel by viewModel()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val numColumns = resources.getInteger(R.integer.phrases_columns)
        val numRows = resources.getInteger(R.integer.phrases_rows)

        val phrases = arguments?.getParcelableArrayList<Phrase>(KEY_PHRASES)
        phrases?.let {
            with(binding.phrasesContainer) {
                layoutManager = GridLayoutManager(requireContext(), numColumns)
                addItemDecoration(
                    ItemOffsetDecoration(
                        requireContext(),
                        R.dimen.speech_button_margin,
                        it.size
                    )
                )

                adapter = PhraseAdapter(it, numRows) { phrase ->
                    presetsViewModel.addToRecents(phrase)
                }

                setHasFixedSize(true)
            }
        }

        return binding.root
    }

    override fun getAllViews(): List<View> = emptyList()
}