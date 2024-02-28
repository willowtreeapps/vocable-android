package com.willowtree.vocable

import com.willowtree.vocable.utils.UUIDProvider

class FakeUUIDProvider : UUIDProvider {

    private var _uuid = 1

    override fun randomUUIDString(): String {
        return _uuid++.toString()
    }
}