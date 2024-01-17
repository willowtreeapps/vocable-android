package com.willowtree.vocable

import com.willowtree.vocable.utils.UUIDProvider

class ConstantUUIDProvider : UUIDProvider {

    var _uuid = "1"

    override fun randomUUIDString(): String = _uuid
}