package com.willowtree.vocable

import com.willowtree.vocable.core.UUIDProvider

class FakeUUIDProvider : UUIDProvider {

    private var _uuid = 1

    override fun randomUUIDString(): String {
        return _uuid++.toString()
    }
}