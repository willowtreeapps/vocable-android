package com.willowtree.vocable.presets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentNumberPadBinding
import com.willowtree.vocable.presets.adapter.PhraseAdapter
import com.willowtree.vocable.room.PhraseDto
import com.willowtree.vocable.utils.ItemOffsetDecoration

class NumberPadFragment : BaseFragment<FragmentNumberPadBinding>() {

    companion object {
        private const val KEY_PHRASES = "KEY_PHRASES"
        const val MAX_PHRASES = 12

        fun newInstance(phrases: List<PhraseDto?>) = NumberPadFragment().apply {
            arguments = bundleOf(KEY_PHRASES to ArrayList(phrases))
        }
    }

    override val bindingInflater: BindingInflater<FragmentNumberPadBinding> =
        FragmentNumberPadBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val numColumns = resources.getInteger(R.integer.number_pad_columns)
        val numRows = resources.getInteger(R.integer.number_pad_rows)

        val phrases = arguments?.getParcelableArrayList<PhraseDto>(KEY_PHRASES)

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
                adapter = PhraseAdapter(
                    it,
                    numRows,
                    null,
                    null
                )
                setHasFixedSize(true)
            }
        }

        return binding.root
    }

    override fun getAllViews() = emptyList<View>()
}