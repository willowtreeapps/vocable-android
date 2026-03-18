package com.willowtree.vocable.core

enum class VocableEnvironmentType {
    PRODUCTION,
    TESTING
}


interface VocableEnvironment {
    val environmentType: VocableEnvironmentType
}

class VocableEnvironmentImpl : VocableEnvironment {
    override val environmentType: VocableEnvironmentType = VocableEnvironmentType.PRODUCTION
}