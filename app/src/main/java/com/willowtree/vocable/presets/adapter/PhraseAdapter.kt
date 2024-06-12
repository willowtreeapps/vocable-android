package com.willowtree.vocable.presets.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.PhraseButtonAddBinding
import com.willowtree.vocable.databinding.PhraseButtonBinding
import com.willowtree.vocable.presets.PhraseGridItem
import java.util.Locale

class PhraseAdapter(
    private val phrases: List<PhraseGridItem>,
    private val numRows: Int,
    private val phraseClickAction: ((String) -> Unit)?,
    private val phraseAddClickAction: (() -> Unit)?
) : RecyclerView.Adapter<PhraseAdapter.PhraseViewHolder>() {

    abstract inner class PhraseViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }

    inner class PhraseGridItemViewHolder(itemView: View) :
        PhraseAdapter.PhraseViewHolder(itemView) {

        override fun bind(position: Int) {
            when (val gridItem = phrases[position]) {
                is PhraseGridItem.Phrase -> {
                    val binding = PhraseButtonBinding.bind(itemView)
                    binding.root.setText(gridItem.text, Locale.getDefault())
                    binding.root.action = {
                        phraseClickAction?.invoke(gridItem.phraseId)
                    }
                }

                PhraseGridItem.AddPhrase -> {
                    val binding = PhraseButtonAddBinding.bind(itemView)
                    binding.root.action = {
                        phraseAddClickAction?.invoke()
                    }
                }
            }
        }
    }

    private var _minHeight: Int? = null

    override fun getItemViewType(position: Int): Int {
        return when (phrases[position]) {
            is PhraseGridItem.Phrase -> R.layout.phrase_button
            PhraseGridItem.AddPhrase -> R.layout.phrase_button_add
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhraseAdapter.PhraseViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        itemView.isInvisible = true
        parent.post {
            with(itemView) {
                minimumHeight = getMinHeight(parent)
                isInvisible = false
            }
        }

        return PhraseGridItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PhraseAdapter.PhraseViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun getMinHeight(parent: ViewGroup): Int {
        if (_minHeight == null) {
            val offset =
                parent.context.resources.getDimensionPixelSize(R.dimen.speech_button_margin)
            _minHeight = (parent.measuredHeight / numRows) - ((numRows - 1) * offset / numRows)
        }
        return _minHeight ?: 0
    }

    override fun getItemCount(): Int = phrases.size
}