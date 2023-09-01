package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.*
import com.willowtree.vocable.databinding.FragmentEditCategoriesBinding
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import kotlin.math.min

class EditCategoriesFragment : BaseFragment<FragmentEditCategoriesBinding>() {

    override val bindingInflater: BindingInflater<FragmentEditCategoriesBinding> = FragmentEditCategoriesBinding::inflate

    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    private val allViews = mutableListOf<View>()
    private var maxEditCategories = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.categoryBackButton.action = {
            when (val currentPosition = binding.editCategoriesViewPager.currentItem) {
                0 -> {
                    binding.editCategoriesViewPager.setCurrentItem(
                        categoriesAdapter.itemCount - 1,
                        true
                    )
                }
                else -> {
                    binding.editCategoriesViewPager.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.categoryForwardButton.action = {
            when (val currentPosition = binding.editCategoriesViewPager.currentItem) {
                categoriesAdapter.itemCount - 1 -> {
                    binding.editCategoriesViewPager.setCurrentItem(0, true)
                }
                else -> {
                    binding.editCategoriesViewPager.setCurrentItem(currentPosition + 1, true)
                }
            }
        }
        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)

        maxEditCategories = resources.getInteger(R.integer.max_edit_categories)

        binding.editCategoriesViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % categoriesAdapter.numPages + 1
                binding.categoryPageNumber.text = getString(
                    R.string.phrases_page_number,
                    pageNum,
                    categoriesAdapter.numPages
                )

                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        binding.backButton.action = {
            findNavController().popBackStack()
        }

        binding.addCategoryButton.action = {
            if (findNavController().currentDestination?.id == R.id.editCategoriesFragment) {
                findNavController().navigate(R.id.action_editCategoriesFragment_to_editCategoriesKeyboardFragment)
            }
        }

        editCategoriesViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory()
            ).get(EditCategoriesViewModel::class.java)
        subscribeToViewModel()

        editCategoriesViewModel.refreshCategories()
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.addRemoveCategoryList.observe(viewLifecycleOwner, Observer {
            it?.let { categories ->
                binding.editCategoriesViewPager.apply {
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
            }
        })

        editCategoriesViewModel.lastViewedIndex.observe(viewLifecycleOwner, Observer {
            it?.let { index ->
                val pageNum = index / maxEditCategories
                val middle = categoriesAdapter.itemCount / 2
                val toScrollTo = if (middle % categoriesAdapter.numPages == 0) {
                    middle + pageNum
                } else {
                    val mod = middle % categoriesAdapter.numPages
                    middle + (categoriesAdapter.numPages - mod) + pageNum
                }
                // Wait until view pager has finished its layout
                binding.editCategoriesViewPager.post {
                    if (isAdded && binding.editCategoriesViewPager.currentItem != toScrollTo) {
                        binding.editCategoriesViewPager.setCurrentItem(toScrollTo, false)
                    }
                }
            }
        })
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<CategoryDto>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<CategoryDto>) {
            super.setItems(items)
            setPagingButtonsEnabled(categoriesAdapter.numPages > 1)
        }

        override fun getMaxItemsPerPage(): Int = maxEditCategories

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * maxEditCategories
            val endPosition = min(items.size, startPosition + maxEditCategories)

            return EditCategoriesListFragment.newInstance(startPosition, endPosition)
        }
    }

    private fun setPagingButtonsEnabled(enable: Boolean) {
        binding.apply {
            categoryForwardButton.isEnabled = enable
            categoryBackButton.isEnabled = enable
            editCategoriesViewPager.isUserInputEnabled = enable
        }
    }

}