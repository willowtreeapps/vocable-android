package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryPhrasesBinding
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.settings.customcategories.CustomCategoryPhraseListFragment
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditCategoryPhrasesFragment : BaseFragment<FragmentEditCategoryPhrasesBinding>() {

    private val args: EditCategoryPhrasesFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentEditCategoryPhrasesBinding> =
        FragmentEditCategoryPhrasesBinding::inflate
    private val editCategoriesViewModel: EditCategoryPhrasesViewModel by viewModel()

    private var maxPhrases = 1
    private lateinit var phrasesAdapter: PhrasesPagerAdapter
    private lateinit var category: Category

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = args.category
        binding.categoryTitle.text = editCategoriesViewModel.getCategoryName(category)
        if (category.categoryId != PresetCategories.RECENTS.id) {
            binding.editOptionsButton.isInvisible = true
        }

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
        }

        if (category.categoryId == PresetCategories.RECENTS.id) {
            binding.addPhraseButton.visibility = View.GONE
        } else {
            binding.addPhraseButton.action = {
                val action =
                    EditCategoryPhrasesFragmentDirections.actionEditCategoryPhrasesFragmentToAddPhraseKeyboardFragment(
                        category
                    )
                if (findNavController().currentDestination?.id == R.id.editCategoryPhrasesFragment) {
                    findNavController().navigate(action)
                }
            }
        }

        val numColumns = resources.getInteger(R.integer.custom_category_phrase_columns)
        val numRows = resources.getInteger(R.integer.custom_category_phrase_rows)

        maxPhrases = numColumns * numRows

        phrasesAdapter = PhrasesPagerAdapter(childFragmentManager)

        binding.editCategoryPagerForwardButton.action = {
            when (val currentPosition = binding.editCategoryPhraseHolder.currentItem) {
                phrasesAdapter.itemCount - 1 -> {
                    binding.editCategoryPhraseHolder.setCurrentItem(0, true)
                }
                else -> {
                    binding.editCategoryPhraseHolder.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.editCategoryPagerBackButton.action = {
            when (val currentPosition = binding.editCategoryPhraseHolder.currentItem) {
                0 -> {
                    binding.editCategoryPhraseHolder.setCurrentItem(
                        phrasesAdapter.itemCount - 1,
                        true
                    )
                }
                else -> {
                    binding.editCategoryPhraseHolder.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.emptyAddPhraseButton.action = {
            val action =
                EditCategoryPhrasesFragmentDirections.actionEditCategoryPhrasesFragmentToAddPhraseKeyboardFragment(
                    category
                )
            if (findNavController().currentDestination?.id == R.id.editCategoryPhrasesFragment) {
                findNavController().navigate(action)
            }
        }

        binding.editCategoryPhraseHolder.apply {
            isSaveEnabled = false
            adapter = phrasesAdapter
            registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    post { updatePageNumber() }
                }
            })
        }

        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.categoryPhraseList.observe(viewLifecycleOwner) {
            it?.let {
                handlePhrases(it)
            }
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun handlePhrases(phrases: List<Phrase>) {
        binding.emptyPhrasesText.isVisible = phrases.isEmpty()
        binding.emptyAddPhraseButton.isVisible = phrases.isEmpty()
        binding.editCategoryPhraseHolder.isVisible = phrases.isNotEmpty()
        binding.editCategoryPagerForwardButton.isVisible = phrases.isNotEmpty()
        binding.editCategoryPagerBackButton.isVisible = phrases.isNotEmpty()
        binding.editCategoryPageNumber.isVisible = phrases.isNotEmpty()

        if (phrases.isNotEmpty()) {
            phrasesAdapter.setItems(phrases)
        }
    }

    private fun updatePageNumber() {
        val currentPage = binding.editCategoryPhraseHolder.currentItem
        val pageNum = currentPage % phrasesAdapter.numPages + 1
        binding.editCategoryPageNumber.text = getString(
            R.string.phrases_page_number,
            pageNum,
            phrasesAdapter.numPages
        )
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase>) {
            super.setItems(items)
            setPagingButtonsEnabled(numPages > 1)
            updatePageNumber()
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            with(binding) {
                editCategoryPagerForwardButton.isEnabled = enable
                editCategoryPagerBackButton.isEnabled = enable
            }
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases


        override fun createFragment(position: Int): Fragment {
            val phrases = getItemsByPosition(position)

            return CustomCategoryPhraseListFragment.newInstance(phrases, args.category)
        }

    }
}
