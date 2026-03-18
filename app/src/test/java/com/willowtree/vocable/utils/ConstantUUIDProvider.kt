package com.willowtree.vocable.utils

import com.willowtree.vocable.core.UUIDProvider

class ConstantUUIDProvider : UUIDProvider {

    var _uuid = "1"

    override fun randomUUIDString(): String = _uuid
}