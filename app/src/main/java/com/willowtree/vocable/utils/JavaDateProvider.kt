package com.willowtree.vocable.utils

class JavaDateProvider : DateProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}