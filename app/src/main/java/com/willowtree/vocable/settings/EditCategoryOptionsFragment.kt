package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryOptionsBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject

class EditCategoryOptionsFragment : BaseFragment<FragmentEditCategoryOptionsBinding>() {

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

    override val bindingInflater: BindingInflater<FragmentEditCategoryOptionsBinding> = FragmentEditCategoryOptionsBinding::inflate
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel
    private val localizedResourceUtility: LocalizedResourceUtility by inject()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = arguments?.getParcelable<Category>(KEY_CATEGORY)

        if (category?.isUserGenerated == true) {
            binding.removeCategoryButton.isInvisible = false
            binding.editOptionsButton?.isInvisible = false
        }

        binding.categoryTitle.text = category?.let { localizedResourceUtility.getTextFromCategory(it) }

        category?.let {
            binding.editOptionsButton?.action = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.settings_fragment_container,
                        EditKeyboardFragment.newInstance(category)
                    ).addToBackStack(null)
                    .commit()
            }
        }

        binding.editOptionsBackButton.action = {
            parentFragmentManager.popBackStack()
        }

        binding.showCategorySwitch.action = {
            binding.categoryShowSwitch.isChecked = !binding.categoryShowSwitch.isChecked
        }

        binding.categoryShowSwitch.apply {
            isChecked = category?.hidden?.not() ?: false
            setOnCheckedChangeListener { _, isChecked ->
                category?.let { category ->
                    editCategoriesViewModel.hideShowCategory(category, !isChecked)
                }
            }
        }

        binding.removeCategoryButton.action = {
            setEditButtonsEnabled(false)
            toggleDialogVisibility(true)
            binding.confirmationDialog.apply {
                dialogTitle.text = resources.getString(R.string.are_you_sure)
                dialogMessage.text = getString(R.string.removed_cant_be_restored)
                dialogPositiveButton.text =
                    resources.getString(R.string.settings_dialog_continue)
                dialogPositiveButton.action = {
                    category?.let {
                        editCategoriesViewModel.deleteCategory(category)
                    }

                    parentFragmentManager.popBackStack()
                }
                dialogNegativeButton.text = resources.getString(R.string.settings_dialog_cancel)
                dialogNegativeButton.action = {
                    toggleDialogVisibility(false)
                    setEditButtonsEnabled(true)
                }
            }
        }


        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoriesViewModel::class.java)
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.confirmationDialog.root.isVisible = visible
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.apply {
            showCategorySwitch.isEnabled = enabled
            editOptionsButton?.isEnabled = enabled
            editOptionsBackButton.isEnabled = enabled
            removeCategoryButton.isEnabled = enabled
            categoryShowSwitch.isEnabled = enabled
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }
}