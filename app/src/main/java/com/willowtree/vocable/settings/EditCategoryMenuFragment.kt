package com.willowtree.vocable.settings

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.willowtree.vocable.databinding.FragmentEditCategoryOptionsBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.android.ext.android.inject
import java.util.function.LongToDoubleFunction

class EditCategoryMenuFragment : BaseFragment<FragmentEditCategoryMenuBinding>() {

    private val args: EditCategoryMenuFragmentArgs by navArgs()


    override val bindingInflater: BindingInflater<FragmentEditCategoryMenuBinding> =
        FragmentEditCategoryMenuBinding::inflate

    private val localizedResourceUtility: LocalizedResourceUtility by inject()
    private lateinit var editCategoriesViewModel: EditCategoriesViewModel

    private lateinit var category: Category

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        category = args.category
        Log.d("Caroline","category is $category")

        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoriesViewModel::class.java)

        //val categoryText = localizedResourceUtility.getTextFromCategory(category)

        //TEMPORARY -----------
//        binding.categoryTitle.text = (category.localizedName?.get("en_US"))

        setUpEditPhrasesButton()

        editCategoriesViewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory()
        ).get(EditCategoriesViewModel::class.java)

        binding.editOptionsBackButton.action = {
            findNavController().popBackStack()
        }

        setUpShowCategoryButton()
        setUpRemoveCategoryButton()
        setUpRenameCategoryButton()
        subscribeToViewModel()
    }

    private fun subscribeToViewModel() {
        editCategoriesViewModel.orderCategoryList.observe(viewLifecycleOwner, Observer {
            it?.let {
                // Get the most updated category name if the user changed it on the
                // EditCategoriesKeyboardFragment screen
                binding.categoryTitle.text =
                    editCategoriesViewModel.getUpdatedCategoryName(args.category)
                category = editCategoriesViewModel.getUpdatedCategory(args.category)
            }
        })
    }

    private fun setUpShowCategoryButton(){
//        binding.showCategoryButton.action={
//            editCategoriesViewModel.hideShowCategory(category, true)
//        }
    }

    private fun setUpRenameCategoryButton(){

        binding.renameCategoryButton.action = {
            Log.d("Caroline","rename category")
            val action =
                EditCategoryMenuFragmentDirections.actionEditCategoryMenuFragmentToEditCategoriesKeyboardFragment(
                    category
                )

            if (findNavController().currentDestination?.id == R.id.editCategoryMenuFragment) {
                findNavController().navigate(action)
            }
        }
    }

    private fun setUpRemoveCategoryButton(){
        binding.removeCategoryButton?.action = {
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
    }

    private fun toggleDialogVisibility(visible: Boolean) {
        binding.confirmationDialog.root.isVisible = visible
    }

    private fun setEditButtonsEnabled(enabled: Boolean) {
        binding.apply {
            editOptionsBackButton.isEnabled = enabled
            removeCategoryButton?.isEnabled = enabled
        }
    }

    private fun setUpEditPhrasesButton(){
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