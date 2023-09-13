package com.willowtree.vocable.utils

class FakeDateProvider : DateProvider {

    var _currentTimeMillis = 0L

    override fun currentTimeMillis(): Long = _currentTimeMillis
}