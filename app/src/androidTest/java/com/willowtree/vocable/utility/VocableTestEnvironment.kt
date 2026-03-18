package com.willowtree.vocable.utility

import com.willowtree.vocable.core.VocableEnvironment
import com.willowtree.vocable.core.VocableEnvironmentType

class VocableTestEnvironment: VocableEnvironment {
    override val environmentType = VocableEnvironmentType.TESTING
}