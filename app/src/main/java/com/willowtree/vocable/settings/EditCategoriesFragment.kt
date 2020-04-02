package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.MainActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoriesBinding
import com.willowtree.vocable.room.Category
import kotlin.math.ceil
import kotlin.math.min

class EditCategoriesFragment : BaseFragment() {

    private var binding: FragmentEditCategoriesBinding? = null

    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    private val allViews = mutableListOf<View>()
    private var maxEditCategories = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEditCategoriesBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.categoryBackButton?.let {
            it.action = {
                when (val currentPosition = binding?.editCategoriesViewPager?.currentItem) {
                    null -> {
                        // No-op
                    }
                    0 -> {
                        binding?.editCategoriesViewPager?.setCurrentItem(categoriesAdapter.itemCount - 1, true)
                    }
                    else -> {
                        binding?.editCategoriesViewPager?.setCurrentItem(currentPosition - 1, true)
                    }
                }
            }
        }

        binding?.categoryForwardButton?.let {
            it.action = {
                when (val currentPosition = binding?.editCategoriesViewPager?.currentItem) {
                    null -> {
                        // No-op
                    }
                    categoriesAdapter.itemCount - 1 -> {
                        binding?.editCategoriesViewPager?.setCurrentItem(0, true)
                    }
                    else -> {
                        binding?.editCategoriesViewPager?.setCurrentItem(currentPosition + 1, true)
                    }
                }
            }
        }
        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)

        maxEditCategories = resources.getInteger(R.integer.max_edit_categories)

        binding?.editCategoriesViewPager?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % categoriesAdapter.numPages + 1
                binding?.categoryPageNumber?.text = getString(
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
        editCategoriesViewModel =
            ViewModelProviders.of(requireActivity()).get(EditCategoriesViewModel::class.java)
        subscribeToViewModel()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.categoryList.observe(viewLifecycleOwner, Observer {
            it?.let { categories ->
                with(binding?.editCategoriesViewPager) {
                    this?.isSaveEnabled = false
                    this?.adapter = categoriesAdapter
                    categoriesAdapter.setCategories(categories)
                    // Move adapter to middle so user can scroll both directions
                    val middle = categoriesAdapter.itemCount / 2
                    if (middle % categoriesAdapter.numPages == 0) {
                        binding?.editCategoriesViewPager?.setCurrentItem(middle, false)
                    } else {
                        val mod = middle % categoriesAdapter.numPages
                        binding?.editCategoriesViewPager?.setCurrentItem(
                            middle + (categoriesAdapter.numPages - mod),
                            false
                        )
                    }
                }
            }
        })
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        FragmentStateAdapter(fm, viewLifecycleOwner.lifecycle) {

        private val categories = mutableListOf<Category>()

        var numPages = 0

        fun setCategories(categories: List<Category>) {
            with(this.categories) {
                clear()
                addAll(categories)
            }
            numPages = ceil(categories.size / maxEditCategories.toDouble()).toInt()
            notifyDataSetChanged()

            setPagingButtonsEnabled(categoriesAdapter.numPages > 1)
        }

        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }

        override fun createFragment(position: Int): Fragment {
            val startPosition = (position % numPages) * maxEditCategories
            val subList = categories.subList(
                startPosition,
                min(categories.size, startPosition + maxEditCategories)
            )
            return EditCategoriesListFragment.newInstance(subList)
        }
    }

    private fun setPagingButtonsEnabled(enable: Boolean) {
        binding?.let {
            it.categoryForwardButton.isEnabled = enable
            it.categoryBackButton.isEnabled = enable
        }
    }

}