package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryMenuBinding
import com.willowtree.vocable.room.Category

class EditCategoryMenuFragment : BaseFragment<FragmentEditCategoryMenuBinding>() {

    private val args: EditCategoryMenuFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentEditCategoryMenuBinding> =
        FragmentEditCategoryMenuBinding::inflate

    private lateinit var editCategoryMenuViewModel: EditCategoryMenuViewModel

    private lateinit var category: Category

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        category = args.category

        editCategoryMenuViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoryMenuViewModel::class.java)

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
        }

        binding.headTrackingSwitch.isChecked = !category.hidden

        setUpRenameCategoryButton()
        setUpShowCategoryButton()
        setUpEditPhrasesButton()
        setUpRemoveCategoryButton()
        setUpCategoryTitle()

    }

    private fun setUpCategoryTitle() {
        editCategoryMenuViewModel.refreshCategories()
        binding.categoryTitle.text =
            editCategoryMenuViewModel.getUpdatedCategoryName(args.category)
        category = editCategoryMenuViewModel.getUpdatedCategory(args.category)
    }

    private fun setUpShowCategoryButton() {
        binding.headTrackingSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                editCategoryMenuViewModel.hideShowCategory(category, !category.hidden)
            }
        }
    }

    private fun setUpRenameCategoryButton() {
        binding.renameCategoryButton.action = {
            val action =
                EditCategoryMenuFragmentDirections.actionEditCategoryMenuFragmentToEditCategoriesKeyboardFragment(
                    category
                )
            if (findNavController().currentDestination?.id == R.id.editCategoryMenuFragment) {
                findNavController().navigate(action)
            }
        }
    }

    private fun setUpRemoveCategoryButton() {
        binding.removeCategoryButton?.action = {
            setEditButtonsEnabled(false)
            toggleDialogVisibility(true)
            binding.confirmationDialog.apply {
                dialogTitle.text = resources.getString(R.string.are_you_sure)
                dialogMessage.text = getString(R.string.removed_cant_be_restored)
                dialogPositiveButton.text =
                    resources.getString(R.string.delete)
                dialogPositiveButton.action = {
                    editCategoryMenuViewModel.deleteCategory(category)

                    findNavController().popBackStack()
                }
                dialogNegativeButton.text = resources.getString(R.string.settings_dialog_cancel)
                dialogNegativeButton.action = {
                    toggleDialogVisibility(false)
                    setEditButtonsEnabled(true)
                }
            }
        }
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.confirmationDialog.root.isVisible = visible
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.apply {
            editOptionsBackButton.isEnabled = enabled
            removeCategoryButton.isEnabled = enabled
        }
    }

    private fun setUpEditPhrasesButton() {
        binding.editPhrasesButton.action = {
            val action =
                EditCategoryMenuFragmentDirections.actionEditCategoryMenuFragmentToEditCategoryOptionsFragment(
                    category
                )
            if (findNavController().currentDestination?.id == R.id.editCategoryMenuFragment) {
                findNavController().navigate(action)
            }
        }
    }

    override fun getAllViews(): List<View> {
        TODO("Not yet implemented")
    }

}