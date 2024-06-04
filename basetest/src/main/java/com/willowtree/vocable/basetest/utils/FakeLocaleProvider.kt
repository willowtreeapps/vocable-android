package com.willowtree.vocable.basetest.utils

import com.willowtree.vocable.utils.locale.LocaleProvider

class FakeLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String = "en_US"
}
