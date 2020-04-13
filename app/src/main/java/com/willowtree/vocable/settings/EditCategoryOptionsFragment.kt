package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryOptionsBinding
import com.willowtree.vocable.room.Category
import kotlinx.android.synthetic.main.fragment_edit_categories.*
import kotlinx.android.synthetic.main.vocable_confirmation_dialog.*

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

    private lateinit var editCategoriesViewModel: EditCategoriesViewModel
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

        if (category?.isUserGenerated == true) {
            binding?.removeCategoryButton?.isInvisible = false
            binding?.editOptionsButton?.isInvisible = false
        }

        binding?.categoryTitle?.text = category?.getLocalizedText()

        category?.let {
            binding?.editOptionsButton?.action = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.settings_fragment_container,
                        EditKeyboardFragment.newInstance(category)
                    ).addToBackStack(null)
                    .commit()
            }
        }

        binding?.editOptionsBackButton?.action = {
            parentFragmentManager.popBackStack()
        }

        binding?.categoryShowSwitch?.let {
            it.isChecked = category?.hidden?.not() ?: false
            it.setOnCheckedChangeListener { _, isChecked ->
                category?.let { category ->
                    editCategoriesViewModel.hideShowCategory(category, !isChecked)
                }
            }
        }

        binding?.removeCategoryButton?.action = {
            setEditButtonsEnabled(false)
            toggleDialogVisibility(true)
            binding?.confirmationDialog?.let {
                it.dialogTitle.text = resources.getString(R.string.are_you_sure)
                it.dialogMessage.text = getString(R.string.removed_cant_be_restored)
                it.dialogPositiveButton.text = resources.getString(R.string.settings_dialog_continue)
                it.dialogPositiveButton.action = {
                    category?.let {
                        editCategoriesViewModel.deleteCategory(category)
                    }

                    parentFragmentManager.popBackStack()
                }
                it.dialogNegativeButton.text = resources.getString(R.string.settings_dialog_cancel)
                it.dialogNegativeButton.action = {
                    toggleDialogVisibility(false)
                    setEditButtonsEnabled(true)
                }
            }
        }


        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(EditCategoriesViewModel::class.java)
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding?.confirmationDialog?.root?.let {
            it.isVisible = visible
        }
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding?.let {
            it.showCategorySwitch.isEnabled = enabled
            it.editOptionsButton?.isEnabled = enabled
            it.editOptionsBackButton.isEnabled = enabled
            it.removeCategoryButton.isEnabled = enabled
            it.categoryShowSwitch.isEnabled = enabled
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