package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
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

class EditCategoryOptionsFragment : BaseFragment<FragmentEditCategoryOptionsBinding>() {

    private val args: EditCategoryOptionsFragmentArgs by navArgs()

    override val bindingInflater: BindingInflater<FragmentEditCategoryOptionsBinding> =
        FragmentEditCategoryOptionsBinding::inflate
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val category = args.category

        if (category.isUserGenerated) {
            binding.removeCategoryButton.isInvisible = false
            binding.editOptionsButton?.isInvisible = false
        }

        category.let {
            binding.editOptionsButton?.action = {
                val action =
                    EditCategoryOptionsFragmentDirections.actionEditCategoryOptionsFragmentToEditCategoriesKeyboardFragment(
                        category
                    )
                findNavController().navigate(action)
            }
        }

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
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

        binding.addPhraseButton.action = {
            val action = EditCategoryOptionsFragmentDirections.actionEditCategoryOptionsFragmentToAddPhraseKeyboardFragment(category)
            findNavController().navigate(action)
        }

        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoriesViewModel::class.java)

        subscribeToViewModel()

        editCategoriesViewModel.refreshCategories()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.orderCategoryList.observe(viewLifecycleOwner, Observer {
            it?.let {
                // Get the most updated category name if the user changed it on the
                // EditCategoriesKeyboardFragment screen
                binding.categoryTitle.text =
                    editCategoriesViewModel.getUpdatedCategoryName(args.category)
            }
        })
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.confirmationDialog.root.isVisible = visible
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.apply {
            editOptionsButton.isEnabled = enabled
            editOptionsBackButton.isEnabled = enabled
            removeCategoryButton.isEnabled = enabled
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }
}