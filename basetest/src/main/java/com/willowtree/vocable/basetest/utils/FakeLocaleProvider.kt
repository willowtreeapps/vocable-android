package com.willowtree.vocable.basetest.utils

import com.willowtree.vocable.core.locale.LocaleProvider

class FakeLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String = "en_US"
}
