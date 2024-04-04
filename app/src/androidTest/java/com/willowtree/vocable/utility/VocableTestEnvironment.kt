package com.willowtree.vocable.utility

import com.willowtree.vocable.utils.VocableEnvironment
import com.willowtree.vocable.utils.VocableEnvironmentType

class VocableTestEnvironment: VocableEnvironment {
    override val environmentType = VocableEnvironmentType.TESTING
}