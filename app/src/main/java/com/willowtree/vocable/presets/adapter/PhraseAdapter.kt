package com.willowtree.vocable.presets.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.PhraseButtonAddBinding
import com.willowtree.vocable.databinding.PhraseButtonBinding
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.locale.LocalizedResourceUtility
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class PhraseAdapter(
    private val phrases: List<Phrase?>,
    private val numRows: Int,
    private val phraseClickAction: ((Phrase) -> Unit)?,
    private val phraseAddClickAction: (() -> Unit)?
) :
    RecyclerView.Adapter<PhraseAdapter.PhraseViewHolder>(), KoinComponent {

    abstract inner class PhraseViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(text: String, position: Int)
    }

    inner class PhraseItemViewHolder(itemView: View) : PhraseAdapter.PhraseViewHolder(itemView) {

        override fun bind(text: String, position: Int) {
            val binding = PhraseButtonBinding.bind(itemView)
            binding.root.setText(text, Locale.getDefault())
            binding.root.action = {
                phrases[position]?.let { phraseClickAction?.invoke(it) }
            }
        }
    }

    inner class PhraseAddItemViewHolder(itemView: View) : PhraseAdapter.PhraseViewHolder(itemView) {

        override fun bind(text: String, position: Int) {
            val binding = PhraseButtonAddBinding.bind(itemView)
            binding.root.action = {
                phraseAddClickAction?.invoke()
            }
        }
    }

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private var _minHeight: Int? = null

    override fun getItemViewType(position: Int): Int {
        return if (phrases[position] == null) {
            R.layout.phrase_button_add
        } else {
            R.layout.phrase_button
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
        return if (viewType == R.layout.phrase_button_add) {
            PhraseAddItemViewHolder(itemView)
        } else {
            PhraseItemViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: PhraseAdapter.PhraseViewHolder, position: Int) {
        val text = localizedResourceUtility.getTextFromPhrase(phrases[position])
        holder.bind(text, position)
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