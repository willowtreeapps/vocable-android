package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditPhrasesBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.settings.customcategories.CustomCategoryPhraseListFragment
import com.willowtree.vocable.utils.VocableFragmentStateAdapter

class EditPhrasesFragment : BaseFragment<FragmentEditPhrasesBinding>() {

    private val args: EditCategoryOptionsFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentEditPhrasesBinding> =
        FragmentEditPhrasesBinding::inflate
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    private var maxPhrases = 1
    private lateinit var phrasesAdapter: PhrasesPagerAdapter
    private lateinit var category: Category

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = args.category

        binding.backButton.action = {
            findNavController().popBackStack()
        }

        binding.addPhraseButton.action = {
            val action =
                EditPhrasesFragmentDirections.actionEditPhrasesFragmentToAddPhraseKeyboardFragment(category)
            if (findNavController().currentDestination?.id == R.id.editPhrasesFragment) {
                findNavController().navigate(action)
            }
        }

        val numColumns = resources.getInteger(R.integer.custom_category_phrase_columns)
        val numRows = resources.getInteger(R.integer.custom_category_phrase_rows)
        maxPhrases = numColumns * numRows

        phrasesAdapter = PhrasesPagerAdapter(childFragmentManager)

        binding.phraseForwardButton.action = {
            when (val currentPosition = binding.editPhrasesViewPager.currentItem) {
                phrasesAdapter.itemCount - 1 -> {
                    binding.editPhrasesViewPager.setCurrentItem(0, true)
                }
                else -> {
                    binding.editPhrasesViewPager.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.phraseBackButton.action = {
            when (val currentPosition = binding.editPhrasesViewPager.currentItem) {
                0 -> {
                    binding.editPhrasesViewPager.setCurrentItem(
                        phrasesAdapter.itemCount - 1,
                        true
                    )
                }
                else -> {
                    binding.editPhrasesViewPager.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.emptyAddPhraseButton.action = {
            val action = EditPhrasesFragmentDirections.actionEditPhrasesFragmentToAddPhraseKeyboardFragment(category)
            if (findNavController().currentDestination?.id == R.id.editPhrasesFragment) {
                findNavController().navigate(action)
            }
        }

        binding.editPhrasesViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % phrasesAdapter.numPages + 1
                binding.phrasePageNumber.text = getString(
                    R.string.phrases_page_number,
                    pageNum,
                    phrasesAdapter.numPages
                )
            }
        })

        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoriesViewModel::class.java)

        subscribeToViewModel()

        with(editCategoriesViewModel) {
            refreshCategories()
            fetchCategoryPhrases(args.category)
        }
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.orderCategoryList.observe(viewLifecycleOwner, Observer {
            it?.let {
                // Get the most updated category name if the user changed it on the
                // EditCategoriesKeyboardFragment screen
                binding.categoryNameTitle.text =
                    editCategoriesViewModel.getUpdatedCategoryName(args.category)
                category = editCategoriesViewModel.getUpdatedCategory(args.category)
            }
        })

        editCategoriesViewModel.categoryPhraseList.observe(viewLifecycleOwner, Observer {
            it?.let {
                handlePhrases(it)
            }
        })
    }

//    private fun toggleDialogVisibility(visible: Boolean) {
//        binding.confirmationDialog.root.isVisible = visible
//    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.backButton.isEnabled = enabled
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun handlePhrases(phrases: List<Phrase>) {
        binding.emptyPhrasesText.isVisible = phrases.isEmpty()
        binding.emptyAddPhraseButton.isVisible = phrases.isEmpty()
        binding.editPhrasesViewPager.isVisible = phrases.isNotEmpty()
        binding.phraseForwardButton.isVisible = phrases.isNotEmpty()
        binding.phraseBackButton.isVisible = phrases.isNotEmpty()
        binding.phrasePageNumber.isVisible = phrases.isNotEmpty()

        if (phrases.isNotEmpty()) {
            with(binding.editPhrasesViewPager) {
                isSaveEnabled = false

                adapter = phrasesAdapter

                phrasesAdapter.setItems(phrases)

                // Move adapter to middle so user can scroll both directions
                val middle = phrasesAdapter.itemCount / 2
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
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase>) {
            super.setItems(items)
            setPagingButtonsEnabled(numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding.apply {
                phraseForwardButton.isEnabled = enable
                phraseBackButton.isEnabled = enable

            }
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases

        override fun createFragment(position: Int): Fragment {
            val phrases = getItemsByPosition(position)

            return CustomCategoryPhraseListFragment.newInstance(phrases, args.category)
        }

    }
}