package com.willowtree.vocable.utility

import com.willowtree.vocable.core.DateProvider

class FakeDateProvider : DateProvider {

    var time = 0L

    override fun currentTimeMillis(): Long {
        return time
    }
}