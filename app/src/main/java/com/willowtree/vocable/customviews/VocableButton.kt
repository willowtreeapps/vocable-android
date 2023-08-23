package com.willowtree.vocable.customviews

import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatButton
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

/**
 * A custom AppCompatButton that will delay for two seconds when a pointer enters and then will call
 * VocableTextToSpeech to say the button text aloud and then perform an action based on the
 * subclass's implementation
 */
open class VocableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatButton(context, attrs, defStyle),
    PointerListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    KoinComponent {

    companion object {
        private const val SINGLE_SPACE = " "
        private const val NO_TEXT_START = 0
        private const val NO_TEXT_END = 1
    }

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val sharedPrefs: VocableSharedPreferences by inject()
    protected var dwellTime: Long
    protected var locale: Locale = Locale.ENGLISH

    init {
        dwellTime = sharedPrefs.getDwellTime()
        setOnClickListener {
            sayText(text)
            performAction()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isSelected = true
            }

            delay(dwellTime)

            uiScope.launch {
                isSelected = false
                isPressed = true
                sayText(text)
                performAction()
            }
        }
    }

    fun setIconWithNoText(@DrawableRes icon: Int) {
        val imageSpan = ImageSpan(
            context,
            icon,
            DynamicDrawableSpan.ALIGN_BOTTOM
        )
        val stringBuilder = SpannableStringBuilder().apply {
            append(SINGLE_SPACE)
            setSpan(imageSpan, NO_TEXT_START, NO_TEXT_END, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setText(stringBuilder, BufferType.SPANNABLE)
    }

    /**
     * Sets the text and the language of the text to use for the text-to-speech engine
     *
     * @param text The user-visible text to set on the button
     * @param locale The locale to be used for text-to-speech
     */
    fun setText(text: String, locale: Locale) {
        this.locale = locale
        setText(text)
    }

    protected open fun sayText(text: CharSequence?) {
        text?.let {
            VocableTextToSpeech.speak(locale, it.toString())
            SpokenText.postValue(it.toString())
        }
    }

    protected open fun performAction() {
        // No-op in the base class
    }

    override fun onPointerExit() {
        buttonJob?.cancel()
        isPressed = false
        isSelected = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        buttonJob?.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            VocableSharedPreferences.KEY_DWELL_TIME -> {
                dwellTime = sharedPrefs.getDwellTime()
            }
        }
    }
}