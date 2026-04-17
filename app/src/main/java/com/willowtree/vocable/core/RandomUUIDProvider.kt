package com.willowtree.vocable.core

import java.util.UUID

class RandomUUIDProvider : UUIDProvider {
    override fun randomUUIDString(): String {
        return UUID.randomUUID().toString()
    }
}