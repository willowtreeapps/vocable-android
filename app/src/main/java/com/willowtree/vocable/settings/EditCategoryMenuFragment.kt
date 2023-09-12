package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.willowtree.vocable.BaseFragment
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.NoSayTextButton
import com.willowtree.vocable.databinding.FragmentEditCategoryMenuBinding
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject

class EditCategoryMenuFragment : BaseFragment<FragmentEditCategoryMenuBinding>() {

    private val args: EditCategoryMenuFragmentArgs by navArgs()

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    override val bindingInflater: BindingInflater<FragmentEditCategoryMenuBinding> =
        FragmentEditCategoryMenuBinding::inflate

    private val editCategoryMenuViewModel: EditCategoryMenuViewModel by viewModels({ requireActivity() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editCategoryMenuViewModel.updateCategoryById(args.category.categoryId)

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
        }

        editCategoryMenuViewModel.currentCategory.observe(viewLifecycleOwner) {
            setUpRenameCategoryButton()
            setUpShowCategoryButton()
            setUpEditPhrasesButton()
            setUpRemoveCategoryButton()
            setUpCategoryTitle()
        }
    }

    private fun setUpCategoryTitle() {
        editCategoryMenuViewModel.currentCategory.observe(viewLifecycleOwner, Observer {
            binding.categoryTitle.text = localizedResourceUtility.getTextFromCategory(it)
        })
    }

    private fun setUpShowCategoryButton() {
        editCategoryMenuViewModel.currentCategory.observe(viewLifecycleOwner, Observer {
            binding.showCategorySwitch.isChecked = !it.hidden
        })
        binding.showCategorySwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                editCategoryMenuViewModel.updateHiddenStatus(isChecked)
            }
        }
    }

    private fun setUpRenameCategoryButton() {
        (binding.renameCategoryButton as NoSayTextButton).action = {
            val action =
                EditCategoryMenuFragmentDirections.actionEditCategoryMenuFragmentToEditCategoriesKeyboardFragment(
                    editCategoryMenuViewModel.currentCategory.value
                )
            if (findNavController().currentDestination?.id == R.id.editCategoryMenuFragment) {
                findNavController().navigate(action)
            }
        }
    }

    private fun setUpRemoveCategoryButton() {
        (binding.removeCategoryButton).action = {
            setEditButtonsEnabled(false)
            toggleDialogVisibility(true)
            binding.confirmationDialog.apply {
                dialogTitle.text = resources.getString(R.string.are_you_sure)
                dialogMessage.text = getString(R.string.removed_cant_be_restored)
                dialogPositiveButton.text =
                    resources.getString(R.string.delete)
                dialogPositiveButton.action = {
                    editCategoryMenuViewModel.deleteCategory()

                    findNavController().popBackStack()
                }
                dialogNegativeButton.text = resources.getString(R.string.settings_dialog_cancel)
                dialogNegativeButton.action = {
                    toggleDialogVisibility(false)
                    setEditButtonsEnabled(true)
                }
            }
        }
        binding.removeCategoryButton.isEnabled = editCategoryMenuViewModel.lastCategoryRemaining.value == false
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
        if (editCategoryMenuViewModel.currentCategory.value?.categoryId == PresetCategories.RECENTS.id || editCategoryMenuViewModel.currentCategory.value?.categoryId == PresetCategories.USER_KEYPAD.id) {
            binding.editPhrasesButton.isEnabled = false
        } else {
            binding.editPhrasesButton.isEnabled = true
            (binding.editPhrasesButton as NoSayTextButton).action  = {
                val action =
                    EditCategoryMenuFragmentDirections.actionEditCategoryMenuFragmentToEditCategoryPhrasesFragment(
                        editCategoryMenuViewModel.currentCategory.value ?: args.category
                    )
                if (findNavController().currentDestination?.id == R.id.editCategoryMenuFragment) {
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun getAllViews(): List<View> {
        return emptyList()
    }

}
