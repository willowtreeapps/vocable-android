package com

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.willowtree.vocable.VocableTestApp

class VocableTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, VocableTestApp::class.simpleName, context)
    }
}