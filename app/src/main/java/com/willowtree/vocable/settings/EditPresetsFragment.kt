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
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentEditPresetsBinding
import com.willowtree.vocable.presets.MySayingsEmptyFragment
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.VocableFragmentStateAdapter

class EditPresetsFragment : BaseFragment<FragmentEditPresetsBinding>() {

    override val bindingInflater: BindingInflater<FragmentEditPresetsBinding> = FragmentEditPresetsBinding::inflate
    private var allViews = mutableListOf<View>()

    private var maxPhrases = 1

    private lateinit var editPhrasesViewModel: EditPhrasesViewModel
    private lateinit var phrasesAdapter: EditPhrasesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maxPhrases = resources.getInteger(R.integer.max_edit_phrases)

        binding.backButton.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding.phrasesForwardButton.action = {
            when (val currentPosition = binding.editSayingsViewPager.currentItem) {
                phrasesAdapter.itemCount - 1 -> {
                    binding.editSayingsViewPager.setCurrentItem(0, true)
                }
                else -> {
                    binding.editSayingsViewPager.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.phrasesBackButton.action = {
            when (val currentPosition = binding.editSayingsViewPager.currentItem) {
                0 -> {
                    binding.editSayingsViewPager.setCurrentItem(
                        phrasesAdapter.itemCount - 1,
                        true
                    )
                }
                else -> {
                    binding.editSayingsViewPager.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        phrasesAdapter = EditPhrasesAdapter(childFragmentManager)

        binding.editSayingsViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.phrasesPageNumber.post {
                    val pageNum = position % phrasesAdapter.numPages + 1
                    binding.phrasesPageNumber.text = getString(
                        R.string.phrases_page_number,
                        pageNum,
                        phrasesAdapter.numPages
                    )
                }

                activity?.let { activity ->
                    allViews.clear()
                    if (activity is SettingsActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        binding.addSayingsButton.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings_fragment_container,
                    EditPhrasesKeyboardFragment.newInstance(null)
                )
                .addToBackStack(null)
                .commit()
        }

        editPhrasesViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(EditPhrasesViewModel::class.java)
        subscribeToViewModel()

    }

    private fun subscribeToViewModel() {
        editPhrasesViewModel.mySayingsList.observe(viewLifecycleOwner, Observer {
            it?.let { phrases ->
                binding.editSayingsViewPager.apply {
                    adapter = phrasesAdapter
                    phrasesAdapter.setItems(phrases)
                    // Move adapter to middle so user can scroll both directions
                    val middle = phrasesAdapter.itemCount / 2
                    if (phrasesAdapter.numPages == 0) {
                        phrasesAdapter.numPages = 1
                    }
                    if (middle % phrasesAdapter.numPages == 0) {
                        setCurrentItem(middle, false)
                    } else {
                        val mod = middle % phrasesAdapter.numPages
                        setCurrentItem(
                            middle + (phrasesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })

        editPhrasesViewModel.setButtonEnabled.observe(viewLifecycleOwner, Observer { enable ->
            binding.apply {
                backButton.isEnabled = enable
                addSayingsButton.isEnabled = enable
                phrasesForwardButton.isEnabled = enable
                phrasesBackButton.isEnabled = enable
            }
        })
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.presetsParent)
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

    inner class EditPhrasesAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase>) {
            super.setItems(items)
            setPagingButtonsEnabled(numPages > 1)
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding.apply {
                phrasesForwardButton.isEnabled = enable
                phrasesBackButton.isEnabled = enable
                editSayingsViewPager.isUserInputEnabled = enable
            }
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * maxPhrases
            val sublist = items.subList(
                startPosition,
                items.size.coerceAtMost(startPosition + maxPhrases)
            )

            return if (items.isEmpty()) {
                MySayingsEmptyFragment.newInstance(true)
            } else {
                EditPhrasesFragment.newInstance(sublist)
            }

        }
    }
}