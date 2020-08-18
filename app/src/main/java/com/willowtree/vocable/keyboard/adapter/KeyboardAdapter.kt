package com.willowtree.vocable.keyboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R
import com.willowtree.vocable.databinding.KeyboardKeyLayoutBinding

class KeyboardAdapter(
    private val keys: Array<String>,
    private val keyAction: (keyText: String) -> Unit,
    private val numRows: Int
) : RecyclerView.Adapter<KeyboardAdapter.KeyboardKeyViewHolder>() {

    class KeyboardKeyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = KeyboardKeyLayoutBinding.bind(itemView)

        fun bind(keyText: String, keyAction: (keyText: String) -> Unit) {
            with(binding.root) {
                action = {
                    keyAction(keyText)
                }
                text = keyText
            }
        }
    }

    private var _minHeight: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyboardKeyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.keyboard_key_layout, parent, false)
        itemView.isInvisible = true
        parent.post {
            with(itemView) {
                minimumHeight = getMinHeight(parent)
                isInvisible = false
            }
        }
        return KeyboardKeyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: KeyboardKeyViewHolder, position: Int) {
        val keyText = keys[position]
        holder.bind(keyText, keyAction)
    }

    private fun getMinHeight(parent: ViewGroup): Int {
        if (_minHeight == null) {
            val offset =
                parent.context.resources.getDimensionPixelSize(R.dimen.keyboard_key_margin)
            _minHeight = (parent.measuredHeight / numRows) - ((numRows - 1) * offset / numRows)
        }
        return _minHeight ?: 0
    }

    override fun getItemCount(): Int = keys.size
}