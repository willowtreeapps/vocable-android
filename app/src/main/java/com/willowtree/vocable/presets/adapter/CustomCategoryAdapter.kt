package com.willowtree.vocable.presets.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.CustomCategorySwitchItemBinding
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.LocalizedResourceUtility
import org.koin.core.KoinComponent
import org.koin.core.inject

class CustomCategoryAdapter(
    private var categories: Map<Category, Boolean>,
    private val numRows: Int,
    private val onCategoryToggle: (Category, Boolean) -> Unit
) : RecyclerView.Adapter<CustomCategoryAdapter.CustomCategoryViewHolder>(), KoinComponent {

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private var _minHeight: Int? = null

    inner class CustomCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding: CustomCategorySwitchItemBinding =
            CustomCategorySwitchItemBinding.bind(itemView)

        fun bind(category: Category, isChecked: Boolean, onCategoryToggle: (Category, Boolean) -> Unit) {
//            binding.categoryText.text = localizedResourceUtility.getTextFromCategory(category)

//            binding.categoryContainer.action = { binding.toggleSwitch.isChecked = !binding.toggleSwitch.isChecked }

            binding.toggleSwitch.isChecked = isChecked

            binding.toggleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                onCategoryToggle(category, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomCategoryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.custom_category_switch_item, parent, false)
        itemView.isInvisible = true
        parent.post {
            with(itemView) {
                findViewById<View>(R.id.filler_view).minimumHeight = getMinHeight(parent)
                isInvisible = false
            }
        }

        return CustomCategoryViewHolder(itemView)
    }

    private fun getMinHeight(parent: ViewGroup): Int {
        if (_minHeight == null) {
            val offset =
                parent.context.resources.getDimensionPixelSize(R.dimen.edit_category_phrase_button_margin)
            _minHeight = (parent.measuredHeight / numRows) - ((numRows - 1) * offset / numRows)
        }

        return _minHeight ?: 0
    }

    fun setMap(map: Map<Category, Boolean>) {
        this.categories = map
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CustomCategoryViewHolder, position: Int) {
        val keys = ArrayList(this.categories.keys)
        val values = ArrayList(this.categories.values)
        holder.bind(keys[position], values[position], onCategoryToggle)
    }
}