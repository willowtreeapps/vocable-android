package com.willowtree.vocable

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(getLayout(), container, false)
    }

    @LayoutRes
    protected abstract fun getLayout(): Int

    abstract fun getAllViews(): List<View>

    protected fun buildTextWithIcon(
        vararg strings: String,
        iconCharStart: Int,
        iconCharEnd: Int,
        view: TextView,
        icon: Int
    ) {
        val sBuilder = SpannableStringBuilder()

        for (item in strings) {
            sBuilder.append(item)
        }
        val imageSpan = ImageSpan(
            requireContext(),
            icon,
            DynamicDrawableSpan.ALIGN_BOTTOM
        )
        sBuilder.setSpan(imageSpan, iconCharStart, iconCharEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        view.setText(sBuilder, TextView.BufferType.SPANNABLE)
    }
}