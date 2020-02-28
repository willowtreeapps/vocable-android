package com.willowtree.vocable

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    abstract fun getAllViews(): List<View>

    protected fun buildTextWithIcon(
        vararg strings: String,
        iconCharStart: Int,
        iconCharEnd: Int,
        view: TextView,
        @DrawableRes icon: Int
    ) {
        val sBuilder = SpannableStringBuilder()

        for (item in strings) {
            sBuilder.append(item)
        }
        val imageSpan = ImageSpan(
            requireContext(),
            icon,
            DynamicDrawableSpan.ALIGN_BASELINE
        )
        sBuilder.setSpan(imageSpan, iconCharStart, iconCharEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        view.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }
}