package com.willowtree.vocable.settings

import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.willowtree.vocable.databinding.FragmentEditCategoryMenuBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.inject

class EditCategoryMenuFragment : BaseFragment<FragmentEditCategoryMenuBinding>() {

    private val args: EditCategoryMenuFragmentArgs by navArgs()

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    override val bindingInflater: BindingInflater<FragmentEditCategoryMenuBinding> =
        FragmentEditCategoryMenuBinding::inflate

    private lateinit var editCategoryMenuViewModel: EditCategoryMenuViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editCategoryMenuViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoryMenuViewModel::class.java)

        editCategoryMenuViewModel.updateCategoryById(args.category.categoryId)
        editCategoryMenuViewModel.retrieveShowStatus()

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
        }

        setUpRenameCategoryButton()
        setUpShowCategoryButton()
        setUpEditPhrasesButton()
        setUpRemoveCategoryButton()
        setUpCategoryTitle()

    }

    private fun setUpCategoryTitle() {
        editCategoryMenuViewModel.currentCategory.observe(viewLifecycleOwner, Observer {
            binding.categoryTitle.text = localizedResourceUtility.getTextFromCategory(it)
        })
    }

    private fun setUpShowCategoryButton() {
        editCategoryMenuViewModel.showCategoryStatus.observe(viewLifecycleOwner, Observer {
            binding.showCategorySwitch.isChecked = it
        })
        binding.showCategorySwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                editCategoryMenuViewModel.updateHiddenStatus(isChecked)
            }
        }
    }

    private fun setUpRenameCategoryButton() {
        binding.renameCategoryButton.action = {
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
        binding.removeCategoryButton?.action = {
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
                    editCategoryMenuViewModel.currentCategory.value?:args.category
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