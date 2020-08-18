package com.willowtree.vocable.presets.adapter

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.willowtree.vocable.R

class PhraseItemOffsetDecoration(
    context: Context,
    private val numColumns: Int,
    private val itemCount: Int
) : RecyclerView.ItemDecoration() {

    private var offset = 0

    init {
        offset = context.resources.getDimensionPixelSize(R.dimen.speech_button_margin)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemIndex = parent.indexOfChild(view)

        var rightOffset = offset
        var bottomOffset = offset

        // Remove end offset if item is at end of row
        if (itemIndex % numColumns == numColumns - 1) {
            rightOffset = 0
        }
        // Remove bottom offset if item on the last row
        if (itemIndex >= itemCount - numColumns) {
            bottomOffset = 0
        }
        outRect.set(0, 0, rightOffset, bottomOffset)
    }
}