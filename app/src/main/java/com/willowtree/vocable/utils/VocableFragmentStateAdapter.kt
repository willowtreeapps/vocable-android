package com.willowtree.vocable.utils

import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.math.ceil
import kotlin.math.min

/**
 * A custom implementation of FragmentStateAdapter to solve an issue with fragments not being
 * properly updated when new data was added. If the default implementations of getItemId() and
 * containsItem() are left unchanged, the adapter will not update any previously created fragments
 * with new data. This custom class solves this issue by giving unique ids to each fragment and
 * updating the ids every time the data set is changed with setItems().
 */
abstract class VocableFragmentStateAdapter<T>(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    protected val items = mutableListOf<T>()
    private var baseId = 0L
    var numPages = 0

    override fun getItemCount(): Int {
        return if (items.isEmpty() || getMaxItemsPerPage() >= items.size) {
            1
        } else {
            Int.MAX_VALUE
        }
    }

    @CallSuper
    open fun setItems(items: List<T>) {
        baseId = System.currentTimeMillis()
        with(this.items) {
            clear()
            addAll(items)
        }

        numPages = if (items.isEmpty() || getMaxItemsPerPage() >= items.size) {
            1
        } else {
            ceil(items.size / getMaxItemsPerPage().toDouble()).toInt()
        }

        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return baseId - position
    }

    override fun containsItem(itemId: Long): Boolean {
        val position = baseId - itemId
        return position in 0 until itemCount
    }

    /**
     * Returns the list of items held by the page at the provided position.
     */
    fun getItemsByPosition(position: Int): List<T> {
        val pageItems = mutableListOf<T>()
        val startPosition = (position % numPages) * getMaxItemsPerPage()

        pageItems.addAll(items.subList(
            startPosition,
            min(items.size, startPosition + getMaxItemsPerPage())
        ))

        return pageItems
    }

    abstract fun getMaxItemsPerPage(): Int
}