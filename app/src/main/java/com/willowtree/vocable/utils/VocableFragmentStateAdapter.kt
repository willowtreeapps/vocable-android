package com.willowtree.vocable.utils

import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.math.ceil

/**
 * A custom implementation of FragmentStateAdapter to solve an issue with fragments not being
 * properly updated when new data was added. If the default implementations of getItemId() and
 * containsItem() are left unchanged, the adapter will not update any previously created fragments
 * with new data. This custom class solves this issue by giving unique ids to each fragment and
 * updating the ids every time the data set is changed with setItems().
 */
abstract class VocableFragmentStateAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    private val items = mutableListOf<Any>()
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
    open fun setItems(items: List<Any>) {
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

    abstract fun getMaxItemsPerPage(): Int
}