package com.willowtree.vocable.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Adds spacing at the bottom and end of items in a RecyclerView using a GridLayoutManager. Will not
 * add bottom spacing to the last row of items and will not add end spacing to the last column of
 * items.
 *
 * @param context Context used to get the pixel size of the offset
 * @param offsetSize The dimen resource representing the offset
 * @param itemCount The number of items in the RecyclerView
 */
class ItemOffsetDecoration(
    context: Context,
    @DimenRes private val offsetSize: Int,
    private val itemCount: Int
) : RecyclerView.ItemDecoration() {

    private var offset = 0

    init {
        offset = context.resources.getDimensionPixelSize(offsetSize)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemIndex = parent.indexOfChild(view)

        val layoutManager = parent.layoutManager
        val numColumns = if (layoutManager is GridLayoutManager) {
            layoutManager.spanCount
        } else {
            0
        }

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