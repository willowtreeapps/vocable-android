package com.willowtree.vocable.utils

import java.util.UUID

class RandomUUIDProvider : UUIDProvider {
    override fun randomUUIDString(): String {
        return UUID.randomUUID().toString()
    }
}