package com.willowtree.vocable.presets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentPresetsBinding
import com.willowtree.vocable.keyboard.KeyboardFragment
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlin.math.min

class PresetsFragment : BaseFragment() {

    private var binding: FragmentPresetsBinding? = null
    private val allViews = mutableListOf<View>()

    private var maxCategories = 1
    private var maxPhrases = 1

    private lateinit var presetsViewModel: PresetsViewModel
    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var phrasesAdapter: PhrasesPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPresetsBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maxCategories = resources.getInteger(R.integer.max_categories)

        binding?.categoryForwardButton?.let {
            it.action = {
                when (val currentPosition = binding?.categoryView?.currentItem) {
                    null -> {
                        // No-op
                    }
                    categoriesAdapter.itemCount - 1 -> {
                        binding?.categoryView?.setCurrentItem(0, true)
                    }
                    else -> {
                        binding?.categoryView?.setCurrentItem(currentPosition + 1, true)
                    }
                }
            }
        }

        binding?.categoryBackButton?.let {
            it.action = {
                when (val currentPosition = binding?.categoryView?.currentItem) {
                    null -> {
                        // No-op
                    }
                    0 -> {
                        binding?.categoryView?.setCurrentItem(categoriesAdapter.itemCount - 1, true)
                    }
                    else -> {
                        binding?.categoryView?.setCurrentItem(currentPosition - 1, true)
                    }
                }
            }
        }

        binding?.phrasesForwardButton?.let {
            it.action = {
                when (val currentPosition = binding?.phrasesView?.currentItem) {
                    null -> {
                        // No-op
                    }
                    phrasesAdapter.itemCount - 1 -> {
                        binding?.phrasesView?.setCurrentItem(0, true)
                    }
                    else -> {
                        binding?.phrasesView?.setCurrentItem(currentPosition + 1, true)
                    }
                }
            }
        }

        binding?.phrasesBackButton?.let {
            it.action = {
                when (val currentPosition = binding?.phrasesView?.currentItem) {
                    null -> {
                        // No-op
                    }
                    0 -> {
                        binding?.phrasesView?.setCurrentItem(phrasesAdapter.itemCount - 1, true)
                    }
                    else -> {
                        binding?.phrasesView?.setCurrentItem(currentPosition - 1, true)
                    }
                }
            }
        }

        binding?.actionButtonContainer?.keyboardButton?.let {
            it.action = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, KeyboardFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding?.actionButtonContainer?.settingsButton?.let {
            it.action = {
                val intent = Intent(activity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)
        phrasesAdapter = PhrasesPagerAdapter(childFragmentManager)

        binding?.categoryView?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        binding?.phrasesView?.registerOnPageChangeCallback(object :
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
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        SpokenText.postValue(null)

        presetsViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(PresetsViewModel::class.java)
        subscribeToViewModel()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        presetsViewModel.populateCategories()
    }

    private fun subscribeToViewModel() {
        SpokenText.observe(viewLifecycleOwner, Observer {
            binding?.currentText?.text = if (it.isNullOrBlank()) {
                getString(R.string.select_something)
            } else {
                it
            }
        })

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding?.speakerIcon?.isVisible = it ?: false
        })

        presetsViewModel.categoryList.observe(viewLifecycleOwner, Observer {
            it?.let { categories ->
                with(binding?.categoryView) {
                    this?.isSaveEnabled = false
                    this?.adapter = categoriesAdapter
                    categoriesAdapter.setItems(categories)
                    // Move adapter to middle so user can scroll both directions
                    val middle = categoriesAdapter.itemCount / 2
                    if (middle % categoriesAdapter.numPages == 0) {
                        binding?.categoryView?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % categoriesAdapter.numPages
                        binding?.categoryView?.setCurrentItem(
                            middle + (categoriesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })

        presetsViewModel.currentPhrases.observe(viewLifecycleOwner, Observer {
            it?.let { phrases ->
                with(binding?.phrasesView) {
                    this?.isSaveEnabled = false
                    this?.adapter = phrasesAdapter

                    maxPhrases =
                        if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_123_id)) {
                            NumberPadFragment.MAX_PHRASES
                        } else {
                            resources.getInteger(R.integer.max_phrases)
                        }

                    phrasesAdapter.setItems(phrases)
                    // Move adapter to middle so user can scroll both directions
                    val middle = phrasesAdapter.itemCount / 2
                    if (middle % phrasesAdapter.numPages == 0) {
                        binding?.phrasesView?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % phrasesAdapter.numPages
                        binding?.phrasesView?.setCurrentItem(
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

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Category>(fm, viewLifecycleOwner.lifecycle) {

        override fun getMaxItemsPerPage(): Int = maxCategories

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * maxCategories
            val subList = items.subList(
                startPosition,
                min(items.size, startPosition + maxCategories)
            )

            return CategoriesFragment.newInstance(subList)
        }
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase>) {
            super.setItems(items)
            setPagingButtonsEnabled(phrasesAdapter.numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding?.let {
                it.phrasesForwardButton.isEnabled = enable
                it.phrasesBackButton.isEnabled = enable
                it.phrasesView.isUserInputEnabled = enable
            }
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases

        override fun createFragment(position: Int): Fragment {

            val startPosition = (position % numPages) * maxPhrases
            val sublist =
                items.subList(startPosition, min(items.size, startPosition + maxPhrases))

            return if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_123_id)) {
                NumberPadFragment.newInstance(sublist)
            } else if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_my_sayings_id) && items.isEmpty()) {
                MySayingsEmptyFragment.newInstance(false)
            } else {
                PhrasesFragment.newInstance(sublist)
            }
        }

    }
}