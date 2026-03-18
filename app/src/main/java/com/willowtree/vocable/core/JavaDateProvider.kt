package com.willowtree.vocable.core

class JavaDateProvider : DateProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}