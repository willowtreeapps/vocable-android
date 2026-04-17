package com.willowtree.vocable.utils

import com.willowtree.vocable.core.DateProvider

class FakeDateProvider : DateProvider {

    var _currentTimeMillis = 0L

    override fun currentTimeMillis(): Long = _currentTimeMillis
}