package com.willowtree.vocable.settings.customcategories.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.EditCustomCategoryPhraseItemBinding
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CustomCategoryPhraseAdapter(
    private var phrases: List<Phrase>,
    private val numRows: Int,
    private val onPhraseEdit: (Phrase) -> Unit,
    private val onPhraseDelete: (Phrase) -> Unit,
    private val category: Category
) : RecyclerView.Adapter<CustomCategoryPhraseAdapter.CustomCategoryPhraseViewHolder>(),
    KoinComponent {

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private var _minHeight: Int? = null

    inner class CustomCategoryPhraseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding: EditCustomCategoryPhraseItemBinding =
            EditCustomCategoryPhraseItemBinding.bind(itemView)

        fun bind(phrase: Phrase, onPhraseEdit: (Phrase) -> Unit, onPhraseDelete: (Phrase) -> Unit) {
            binding.phraseTextButton?.text = localizedResourceUtility.getTextFromPhrase(phrase)

            //TODO: WILL: check if category is Keypad or Recents
            binding.phraseTextButton?.action = {
                onPhraseEdit(phrase)
            }

            binding.removeCategoryButton?.action = {
                onPhraseDelete(phrase)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomCategoryPhraseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.edit_custom_category_phrase_item, parent, false)
        itemView.isInvisible = true
        parent.post {
            with(itemView) {
                //findViewById<View>(R.id.filler_view).minimumHeight = getMinHeight(parent)
                isInvisible = false
            }
        }

        return CustomCategoryPhraseViewHolder(itemView)
    }

//    private fun getMinHeight(parent: ViewGroup): Int {
//        if (_minHeight == null) {
//            val offset =
//                parent.context.resources.getDimensionPixelSize(R.dimen.edit_category_phrase_button_margin)
//            _minHeight = (parent.measuredHeight / numRows) - ((numRows - 1) * offset / numRows)
//        }
//
//        return _minHeight ?: 0
//    }

    override fun getItemCount(): Int = phrases.size

    override fun onBindViewHolder(holder: CustomCategoryPhraseViewHolder, position: Int) {
        holder.bind(phrases[position], onPhraseEdit, onPhraseDelete)
    }
}