package com.willowtree.vocable.utils

class FakeLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String = "en_US"
}