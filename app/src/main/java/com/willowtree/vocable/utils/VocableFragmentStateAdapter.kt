package com.willowtree.vocable.utils

import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.math.ceil

abstract class VocableFragmentStateAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    private val items = mutableListOf<Any>()
    private var pageIds = listOf<Long>()
    private val emptyId = System.currentTimeMillis()
    var numPages = 0

    override fun getItemCount(): Int = Int.MAX_VALUE

    @CallSuper
    open fun setItems(items: List<Any>) {
        with(this.items) {
            clear()
            addAll(items)
            pageIds = if (items.isEmpty()) {
                listOf(emptyId)
            } else {
                map { it.hashCode().toLong() }
            }
        }

        numPages = if (items.isEmpty()) {
            1
        } else {
            ceil(items.size / getMaxItemsPerPage().toDouble()).toInt()
        }

        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        if (items.isEmpty()) {
            return emptyId
        }
        return pageIds[position % numPages]
    }

    override fun containsItem(itemId: Long): Boolean {
        return pageIds.contains(itemId)
    }

    abstract fun getMaxItemsPerPage(): Int
}