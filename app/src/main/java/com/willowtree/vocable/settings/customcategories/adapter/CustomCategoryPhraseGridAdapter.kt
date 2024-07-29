package com.willowtree.vocable.settings.customcategories.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.EditCustomCategoryPhraseItemBinding
import com.willowtree.vocable.presets.Phrase
import org.koin.core.component.KoinComponent

class CustomCategoryPhraseGridAdapter(
    context: Context,
    private var phrases: List<Phrase>,
    private val onPhraseEdit: (Phrase) -> Unit,
    private val onPhraseDelete: (Phrase) -> Unit
) :
    ArrayAdapter<Phrase>(
        context,
        R.layout.edit_custom_category_phrase_item
    ),
    KoinComponent {

    private lateinit var binding: EditCustomCategoryPhraseItemBinding

    init {
        addAll(phrases)
    }

    // Linter warns about using ViewHolder for smoother scrolling, but we shouldn't be scrolling here anyway
    @SuppressLint("ViewHolder")
    override fun getView(position: Int, itemView: View?, parent: ViewGroup): View {
        binding =
            EditCustomCategoryPhraseItemBinding.inflate(LayoutInflater.from(context), parent, false)

        val listItemView: View = binding.root

        listItemView.isInvisible = true
        binding.removeCategoryButton.action = {
            onPhraseDelete(phrases[position])
        }
        binding.phraseTextButton.action = {
            onPhraseEdit(phrases[position])
        }
        binding.root.setPaddingRelative(
            0,
            context.resources.getDimensionPixelSize(R.dimen.edit_category_phrase_button_margin),
            0,
            0
        )
        binding.phraseTextButton.text = phrases[position].text(context)
        parent.post {
            with(listItemView) {
                isInvisible = false
            }
        }
        return listItemView
    }
}