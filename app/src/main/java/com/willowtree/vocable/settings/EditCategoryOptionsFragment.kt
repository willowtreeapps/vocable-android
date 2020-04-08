package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryOptionsBinding
import com.willowtree.vocable.room.Category

class EditCategoryOptionsFragment : BaseFragment() {

    companion object {
        private const val KEY_CATEGORY = "KEY_CATEGORY"

        fun newInstance(category: Category): EditCategoryOptionsFragment {
            return EditCategoryOptionsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_CATEGORY, category)
                }
            }
        }
    }

    private var binding: FragmentEditCategoryOptionsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentEditCategoryOptionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = arguments?.getParcelable<Category>(KEY_CATEGORY)

        if(category?.isUserGenerated == true) {
            binding?.removeCategoryButton?.isInvisible = false
        }

        binding?.categoryTitle?.text = category?.getLocalizedText()

        binding?.editOptionsButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings_fragment_container,
                    EditKeyboardFragment()
                ).addToBackStack(null)
                .commit()
        }

        binding?.editOptionsBackButton?.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, EditCategoriesListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

}