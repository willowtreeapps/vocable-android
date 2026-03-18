package com.willowtree.vocable.core.locale

import java.util.Locale

class JavaLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String =
        Locale.getDefault().toString()
}