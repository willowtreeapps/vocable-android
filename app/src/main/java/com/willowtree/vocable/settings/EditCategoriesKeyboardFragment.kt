package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.BindingInflater
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.FragmentEditKeyboardBinding
import com.willowtree.vocable.room.Category
import androidx.lifecycle.Observer
import java.util.*

class EditCategoriesKeyboardFragment : EditKeyboardFragment() {

    companion object {
        private const val KEY_CATEGORY = "KEY_CATEGORY"

        fun newInstance(category: Category?) = EditCategoriesKeyboardFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_CATEGORY, category)
            }
        }
    }

    override val bindingInflater: BindingInflater<FragmentEditKeyboardBinding> =
        FragmentEditKeyboardBinding::inflate
    private lateinit var viewModel: EditCategoriesViewModel
    private var category: Category? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        arguments?.getParcelable<Category>(KEY_CATEGORY)?.let {
            category = it
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.action = {
            val categoryTextChanged =
                binding.keyboardInput.text.toString() != category?.getLocalizedText()
            if (!categoryTextChanged || isDefaultTextVisible()) {
                parentFragmentManager.popBackStack()
            } else {
                showConfirmationDialog()
            }
        }


        binding.saveButton.action = {
            if ((category == null) && !isDefaultTextVisible()) {
                binding.keyboardInput.text?.let { text ->
                    if (text.isNotBlank()) {
                        viewModel.addNewCategory(text.toString())
                        parentFragmentManager.popBackStack()
                    }
                }
            } else if ((category != null) && !isDefaultTextVisible()) {
                binding.keyboardInput.text.let { text ->
                    val categoryName = category?.localizedName?.toMutableMap()?.apply {
                        put(Locale.getDefault().toString(), text.toString())
                    }
                    category?.localizedName = categoryName ?: mapOf()
                    category?.let { updatedCategory ->
                        viewModel.updateCategory(updatedCategory)
                    } ?: viewModel.addNewCategory(text.toString())
                }
            }
        }

        val inputText = if (category?.getLocalizedText().isNullOrEmpty()) {
            getString(R.string.keyboard_select_letters)
        } else {
            category?.getLocalizedText()
        }

        binding.keyboardInput.setText(inputText)

        (binding.phraseSavedView.root as TextView).apply {
            if (category != null) {
                setText(R.string.changes_saved)
            }
        }

        viewModel = ViewModelProviders.of(
            requireActivity(),
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(EditCategoriesViewModel::class.java)

        subscribeToViewModel()
    }


    private fun subscribeToViewModel() {
        viewModel.showCategoryAdded.observe(viewLifecycleOwner, Observer {
            binding.phraseSavedView.root.isVisible = it ?: false
        })
    }
}