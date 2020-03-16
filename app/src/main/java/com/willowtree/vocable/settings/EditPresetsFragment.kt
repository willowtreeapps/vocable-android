package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.VocableImageButton
import com.willowtree.vocable.databinding.FragmentEditPresetsBinding
import com.willowtree.vocable.room.Phrase
import java.lang.Math.ceil

class EditPresetsFragment : BaseFragment() {

    private var binding: FragmentEditPresetsBinding? = null
    private var allViews = mutableListOf<View>()

    private var maxPhrases = 1

    private lateinit var editPhrasesViewModel: EditPhrasesViewModel
    private lateinit var phrasesAdapter: EditPhrasesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditPresetsBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maxPhrases = resources.getInteger(R.integer.max_edit_phrases)

        binding?.backButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        // Cast is required for successful build
        (binding?.handsetBackButton as? VocableImageButton)?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding?.phrasesForwardButton?.let {
            it.action = {
                when (val currentPosition = binding?.editSayingsViewPager?.currentItem) {
                    null -> {
                        // no-op
                    }
                    phrasesAdapter.itemCount - 1 -> {
                        binding?.editSayingsViewPager?.setCurrentItem(0, true)
                    }
                    else -> {
                        binding?.editSayingsViewPager?.setCurrentItem(currentPosition + 1, true)
                    }
                }
            }
        }

        binding?.phrasesBackButton?.let {
            it.action = {
                when (val currentPosition = binding?.editSayingsViewPager?.currentItem) {
                    null -> {
                        // No-op
                    }
                    0 -> {
                        binding?.editSayingsViewPager?.setCurrentItem(
                            phrasesAdapter.itemCount - 1,
                            true
                        )
                    }
                    else -> {
                        binding?.editSayingsViewPager?.setCurrentItem(currentPosition - 1, true)
                    }
                }
            }
        }

        phrasesAdapter = EditPhrasesAdapter(childFragmentManager)

        binding?.editSayingsViewPager?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % phrasesAdapter.numPages + 1
                binding?.phrasesPageNumber?.text = getString(
                    R.string.phrases_page_number,
                    pageNum,
                    phrasesAdapter.numPages
                )

                activity?.let { activity ->
                    allViews.clear()
                    if (activity is SettingsActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        editPhrasesViewModel =
            ViewModelProviders.of(requireActivity()).get(EditPhrasesViewModel::class.java)
        subscribeToViewModel()

    }

    private fun subscribeToViewModel() {
        editPhrasesViewModel.mySayingsList.observe(viewLifecycleOwner, Observer {
            it?.let { phrases ->
                with(binding?.editSayingsViewPager) {
                    this?.adapter = phrasesAdapter
                    phrasesAdapter.setPhrases(phrases)
                    // Move adapter to middle so user can scroll both directions
                    val middle = phrasesAdapter.itemCount / 2
                    if (middle % phrasesAdapter.numPages == 0) {
                        binding?.editSayingsViewPager?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % phrasesAdapter.numPages
                        binding?.editSayingsViewPager?.setCurrentItem(
                            middle + (phrasesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding?.presetsParent)
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    inner class EditPhrasesAdapter(fm: FragmentManager) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        private val phrases = mutableListOf<Phrase>()
        var numPages: Int = 0

        fun setPhrases(phrases: List<Phrase>) {
            with(this.phrases) {
                clear()
                addAll(phrases)
            }
            numPages = ceil(phrases.size / maxPhrases.toDouble()).toInt()
            notifyDataSetChanged()

            setPagingButtonsEnabled(phrasesAdapter.numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding?.let {
                it.phrasesForwardButton.isEnabled = enable
                it.phrasesBackButton.isEnabled = enable
            }
        }

        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * maxPhrases
            val sublist = phrases.subList(
                startPosition,
                phrases.size.coerceAtMost(startPosition + maxPhrases)
            )

            return EditPhrasesFragment.newInstance(sublist)
        }

    }
}