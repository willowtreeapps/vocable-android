package com.willowtree.vocable.utility

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.ViewFinder
import org.hamcrest.Matcher
import java.lang.reflect.Field

class SplashScreenIdlingResource(private val viewMatcher: Matcher<View?>?,
                                 private val idleMatcher: Matcher<View?>?
) : IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName() = "Splash Screen Idler"

    override fun isIdleNow(): Boolean {
        val view: View? = getView(viewMatcher)
        val isIdle: Boolean = idleMatcher?.matches(view) ?: false
        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = resourceCallback
    }

    private fun getView(viewMatcher: Matcher<View?>?): View? {
        return try {
            val viewInteraction = onView(viewMatcher)
            val finderField: Field? = viewInteraction.javaClass.getDeclaredField("viewFinder")
            finderField?.isAccessible = true
            val finder = finderField?.get(viewInteraction) as ViewFinder
            finder.view
        } catch (e: Exception) {
            null
        }
    }
}