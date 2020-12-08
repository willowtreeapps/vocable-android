package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditCategoryOptionsBinding
import com.willowtree.vocable.room.Category

class EditCategoryOptionsFragment : BaseFragment<FragmentEditCategoryOptionsBinding>() {

    private val args: EditCategoryOptionsFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentEditCategoryOptionsBinding> =
        FragmentEditCategoryOptionsBinding::inflate
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    private lateinit var category: Category

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = args.category

        binding.editPhrasesButton.isVisible = category.isUserGenerated
        binding.removeCategoryButton.isEnabled = category.isUserGenerated
        binding.editCategoryNameButton.isEnabled = category.isUserGenerated

        category.let {
            binding.editCategoryNameButton.action = {
                val action =
                    EditCategoryOptionsFragmentDirections.actionEditCategoryOptionsFragmentToEditCategoriesKeyboardFragment(
                        category
                    )
                if (findNavController().currentDestination?.id == R.id.editCategoryOptionsFragment) {
                    findNavController().navigate(action)
                }
            }

            binding.showHideButtonSwitch.apply {
                showText.text = "Show"
                toggleSwitch.isChecked = !category.hidden

                showHideSwitch.action = {
                    toggleSwitch.isChecked = !toggleSwitch.isChecked
                }

                toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    // if toggle is checked, the category should show
                    editCategoriesViewModel.hideShowCategory(category, hide = !isChecked)
                }
            }
        }

        binding.editPhrasesButton.action = {

            val action = EditCategoryOptionsFragmentDirections.actionEditCategoryOptionsFragmentToEditPhrasesFragment(category)
            if(findNavController().currentDestination?.id == R.id.editCategoryOptionsFragment) {
                findNavController().navigate(action)
            }

        }

        binding.backButton.action = {
            findNavController().popBackStack()
        }

        binding.removeCategoryButton.action = {
            setEditButtonsEnabled(false)
            toggleDialogVisibility(true)
            binding.confirmationDialog.apply {
                dialogTitle.text = resources.getString(R.string.are_you_sure)
                dialogMessage.text = getString(R.string.removed_cant_be_restored)
                dialogPositiveButton.text =
                    resources.getString(R.string.delete)
                dialogPositiveButton.action = {
                    editCategoriesViewModel.deleteCategory(category)

                    findNavController().popBackStack()
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

        subscribeToViewModel()

        with(editCategoriesViewModel) {
            refreshCategories()
            fetchCategoryPhrases(args.category)
        }
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.orderCategoryList.observe(viewLifecycleOwner, Observer {
            // Get the most updated category name if the user changed it on the
            // EditCategoriesKeyboardFragment screen
            binding.categoryTitle.text =
                editCategoriesViewModel.getUpdatedCategoryName(args.category)
            category = editCategoriesViewModel.getUpdatedCategory(args.category)

        })
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.confirmationDialog.root.isVisible = visible
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.apply {
            editCategoryNameButton.isEnabled = enabled
            backButton.isEnabled = enabled
            removeCategoryButton.isEnabled = enabled
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }
}
