package com.willowtree.vocable.utils.locale

import java.util.Locale

class JavaLocaleProvider : LocaleProvider {
    override fun getDefaultLocaleString(): String =
        Locale.getDefault().toString()
}