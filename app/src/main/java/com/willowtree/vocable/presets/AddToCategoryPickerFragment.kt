package com.willowtree.vocable.presets

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
import com.willowtree.vocable.*
import com.willowtree.vocable.databinding.FragmentAddToCategoryPickerBinding
import com.willowtree.vocable.presets.adapter.AddToCategoryPickerViewModel
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import org.koin.android.ext.android.inject

class AddToCategoryPickerFragment : BaseFragment<FragmentAddToCategoryPickerBinding>() {

    private val args: AddToCategoryPickerFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentAddToCategoryPickerBinding> =
        FragmentAddToCategoryPickerBinding::inflate
    private val addToCategoryPickerViewModel: AddToCategoryPickerViewModel by inject()

    private var maxCategories = 1
    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var phraseText: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        phraseText = args.phraseText

        binding.backButton.action = {
            findNavController().popBackStack()
        }

        val numColumns = resources.getInteger(R.integer.custom_category_columns)
        val numRows = resources.getInteger(R.integer.custom_category_rows)
        maxCategories = numColumns * numRows

        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)

        binding.categoryPagerForwardButton.action = {
            when (val currentPosition = binding.categoryHolder.currentItem) {
                categoriesAdapter.itemCount - 1 -> {
                    binding.categoryHolder.setCurrentItem(0, true)
                }
                else -> {
                    binding.categoryHolder.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.categoryPagerBackButton.action = {
            when (val currentPosition = binding.categoryHolder.currentItem) {
                0 -> {
                    binding.categoryHolder.setCurrentItem(
                        categoriesAdapter.itemCount - 1,
                        true
                    )
                }
                else -> {
                    binding.categoryHolder.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.emptyAddCategoryButton.action = {
            val action = AddToCategoryPickerFragmentDirections.actionAddToCategoryPickerFragmentToEditCategoriesKeyboardFragment(null)
            findNavController().navigate(action)
        }

        binding.categoryHolder.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % categoriesAdapter.numPages + 1
                binding.categoryPageNumber.text = getString(R.string.phrases_page_number, pageNum, categoriesAdapter.numPages)

                val activity = requireActivity()
                if (activity is MainActivity) {
                    activity.resetAllViews()
                }
            }
        })

        subscribeToViewModel()

        addToCategoryPickerViewModel.getCategoryList()
    }

    private fun subscribeToViewModel() {
        addToCategoryPickerViewModel.categoryList.observe(viewLifecycleOwner, Observer {
            handleCategories(it)
        })
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun handleCategories(categories: List<Category>) {
        val categoriesExist = categories.isNotEmpty()

        binding.emptyStateTitle.isVisible = !categoriesExist
        binding.emptyAddCategoryButton.isVisible = !categoriesExist
        binding.categoryPagerBackButton.isEnabled = categoriesExist
        binding.categoryPagerForwardButton.isEnabled = categoriesExist
        binding.categoryHolder.isVisible = categoriesExist

        if (categoriesExist) {
            with(binding.categoryHolder) {
                isSaveEnabled = false

                adapter = categoriesAdapter

                categoriesAdapter.setItems(categories)

                // Move adapter to middle so user can scroll both directions
                val middle = categoriesAdapter.itemCount / 2
                if (middle % categoriesAdapter.numPages == 0) {
                    setCurrentItem(middle, false)
                } else {
                    val mod = middle % categoriesAdapter.numPages
                    setCurrentItem(
                        middle + (categoriesAdapter.numPages - mod),
                        false
                    )
                }
            }
        } else {
            binding.categoryPageNumber.text = getString(R.string.phrases_page_number, 1, 1)
        }
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
            VocableFragmentStateAdapter<Category>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Category>) {
            super.setItems(items)
            setPagingButtonsEnabled(numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            with (binding) {
                categoryPagerBackButton.isEnabled = enable
                categoryPagerForwardButton.isEnabled = enable
            }
        }

        override fun getMaxItemsPerPage(): Int = maxCategories

        override fun createFragment(position: Int): Fragment {
            val categories = getItemsByPosition(position)

            return AddToCategoryPickerListFragment.newInstance(args.phraseText, categories)
        }
    }
}