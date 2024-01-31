package com.willowtree.vocable.utility

import com.willowtree.vocable.utils.DateProvider

class FakeDateProvider : DateProvider {

    var time = 0L

    override fun currentTimeMillis(): Long {
        return time
    }
}