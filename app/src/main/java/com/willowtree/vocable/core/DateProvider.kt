package com.willowtree.vocable.core

interface DateProvider {
    fun currentTimeMillis(): Long
}