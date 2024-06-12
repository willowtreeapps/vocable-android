package com.willowtree.vocable.presets

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentPresetsBinding
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import com.willowtree.vocable.utils.VocableTextToSpeech
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PresetsFragment : BaseFragment<FragmentPresetsBinding>() {

    override val bindingInflater: BindingInflater<FragmentPresetsBinding> =
        FragmentPresetsBinding::inflate
    private val allViews = mutableListOf<View>()

    private var maxCategories = 1
    private var maxPhrases = 1
    private var isPortraitMode = true
    private var isTabletMode = false

    private val presetsViewModel: PresetsViewModel by activityViewModel()
    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var phrasesAdapter: PhrasesPagerAdapter

    private var recentsCategorySelected = false

    @SuppressLint("NullSafeMutableLiveData")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maxCategories = resources.getInteger(R.integer.max_categories)
        isPortraitMode =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        isTabletMode = resources.getBoolean(R.bool.is_tablet)

        binding.categoryForwardButton.action = {
            when (val currentPosition = binding.categoryView.currentItem) {
                categoriesAdapter.itemCount - 1 -> {
                    selectCategory(0)
                }

                else -> {
                    selectCategory(currentPosition + 1)
                }
            }
        }

        binding.categoryBackButton.action = {
            when (val currentPosition = binding.categoryView.currentItem) {
                0 -> {
                    selectCategory(categoriesAdapter.itemCount - 1)
                }

                else -> {
                    selectCategory(currentPosition - 1)
                }
            }
        }

        binding.phrasesForwardButton.action = {
            when (val currentPosition = binding.phrasesView.currentItem) {
                phrasesAdapter.itemCount - 1 -> {
                    binding.phrasesView.setCurrentItem(0, true)
                }

                else -> {
                    binding.phrasesView.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.phrasesBackButton.action = {
            when (val currentPosition = binding.phrasesView.currentItem) {
                0 -> {
                    binding.phrasesView.setCurrentItem(phrasesAdapter.itemCount - 1, true)
                }

                else -> {
                    binding.phrasesView.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.actionButtonContainer.keyboardButton.action = {
            if (findNavController().currentDestination?.id == R.id.presetsFragment) {
                findNavController().navigate(R.id.action_presetsFragment_to_keyboardFragment)
            }
        }

        binding.actionButtonContainer.settingsButton.action = {
            if (findNavController().currentDestination?.id == R.id.presetsFragment) {
                findNavController().navigate(R.id.action_presetsFragment_to_settingsFragment)
            }
        }

        binding.emptyAddPhraseButton.action = {
            val action =
                presetsViewModel.selectedCategory.value?.let { category ->
                    PresetsFragmentDirections.actionPresetsFragmentToAddPhraseKeyboardFragment(
                        category
                    )
                }
            if (action != null) {
                findNavController().navigate(action)
            }
        }

        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)
        phrasesAdapter = PhrasesPagerAdapter(childFragmentManager)

        binding.categoryView.registerOnPageChangeCallback(object :
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

        binding.phrasesView.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {

                val pageNum = position % phrasesAdapter.numPages + 1
                binding.phrasesPageNumber.apply {
                    post {
                        text = getString(
                            R.string.phrases_page_number,
                            pageNum,
                            phrasesAdapter.numPages
                        )
                    }
                }

                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        SpokenText.postValue(null)

        subscribeToViewModel()
    }

    private fun selectCategory(newPosition: Int) {
        binding.categoryView.setCurrentItem(newPosition, true)
        if (isPortraitMode && !isTabletMode) {
            presetsViewModel.onCategorySelected(
                categoriesAdapter.getCategory(
                    newPosition
                ).categoryId
            )
        }
    }

    private fun subscribeToViewModel() {
        SpokenText.observe(viewLifecycleOwner) {
            binding.currentText.text = if (it.isNullOrBlank()) {
                getString(R.string.select_something)
            } else {
                it
            }
        }

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner) {
            binding.speakerIcon.isVisible = it
        }

        presetsViewModel.apply {
            categoryList.observe(viewLifecycleOwner, ::handleCategories)
            currentPhrases.observe(viewLifecycleOwner, ::handlePhrases)
            selectedCategoryLiveData.observe(viewLifecycleOwner, ::handleSelectedCategory)
        }

        presetsViewModel.navToAddPhrase.observe(viewLifecycleOwner) {
            if (it) {
                val action =
                    presetsViewModel.selectedCategory.value?.let { category ->
                        PresetsFragmentDirections.actionPresetsFragmentToAddPhraseKeyboardFragment(
                            category
                        )
                    }
                if (action != null) {
                    findNavController().navigate(action)
                }
            }
        }
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

    private fun handleCategories(categories: List<Category>) {
        with(binding.categoryView) {
            val categoriesExist = categories.isNotEmpty()
            // if there are no categories to show (the user has hidden them all), then show the empty state
            isVisible = categoriesExist
            binding.phrasesView.isVisible = categoriesExist
            binding.phrasesPageNumber.isVisible = categoriesExist
            binding.phrasesBackButton.isVisible = categoriesExist
            binding.phrasesForwardButton.isVisible = categoriesExist
            binding.categoryBackButton.isVisible = categoriesExist
            binding.categoryForwardButton.isVisible = categoriesExist

            binding.emptyCategoriesText.isVisible = !categoriesExist

            isSaveEnabled = false
            adapter = categoriesAdapter
            categoriesAdapter.setItems(categories)
        }
    }

    private fun handleSelectedCategory(selectedCategory: Category?) {
        with(binding.categoryView) {
            for (i in 0 until categoriesAdapter.numPages) {
                val pageCategories = categoriesAdapter.getItemsByPosition(i)

                if (pageCategories.find { it.categoryId == selectedCategory?.categoryId } != null) {
                    setCurrentItem(i, false)
                    break
                }
            }
            recentsCategorySelected =
                selectedCategory?.categoryId == PresetCategories.RECENTS.id
        }
    }

    private fun handlePhrases(phrases: List<Phrase?>) {
        binding.emptyPhrasesText.isVisible =
            phrases.isEmpty() && !recentsCategorySelected && categoriesAdapter.getSize() > 0
        binding.emptyAddPhraseButton.isVisible =
            phrases.isEmpty() && !recentsCategorySelected && categoriesAdapter.getSize() > 0

        binding.noRecentsTitle.isVisible = phrases.isEmpty() && recentsCategorySelected
        binding.noRecentsMessage.isVisible = phrases.isEmpty() && recentsCategorySelected
        binding.clockIcon.isVisible = phrases.isEmpty() && recentsCategorySelected

        binding.phrasesView.apply {
            isSaveEnabled = false
            adapter = phrasesAdapter

            maxPhrases =
                if (presetsViewModel.selectedCategory.value?.categoryId == PresetCategories.USER_KEYPAD.id) {
                    NumberPadFragment.MAX_PHRASES
                } else {
                    resources.getInteger(R.integer.max_phrases)
                }

            phrasesAdapter.setItems(phrases)
        }
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Category>(fm, viewLifecycleOwner.lifecycle) {

        override fun getMaxItemsPerPage(): Int = maxCategories

        override fun createFragment(position: Int) =
            CategoriesFragment.newInstance(getItemsByPosition(position))

        fun getSize(): Int = items.size

        fun getCategory(position: Int): Category {
            return if (position >= items.size) {
                items[position % items.size]
            } else {
                items[position]
            }
        }
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase?>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase?>) {
            super.setItems(items)
            setPagingButtonsEnabled(phrasesAdapter.numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding.apply {
                phrasesForwardButton.isEnabled = enable
                phrasesBackButton.isEnabled = enable
                phrasesView.isUserInputEnabled = enable
            }
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases

        override fun createFragment(position: Int): Fragment {
            val phrases = getItemsByPosition(position)
            return if (presetsViewModel.selectedCategory.value?.categoryId == PresetCategories.USER_KEYPAD.id) {
                NumberPadFragment.newInstance(phrases)
            } else if (presetsViewModel.selectedCategory.value?.categoryId == PresetCategories.MY_SAYINGS.id && items.isEmpty()) {
                MySayingsEmptyFragment.newInstance(false)
            } else {
                PhrasesFragment.newInstance(phrases)
            }
        }

    }
}
