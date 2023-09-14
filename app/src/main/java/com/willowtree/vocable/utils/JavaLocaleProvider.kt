package com.willowtree.vocable.utils

import java.util.Locale

class JavaLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String =
        Locale.getDefault().toString()
}