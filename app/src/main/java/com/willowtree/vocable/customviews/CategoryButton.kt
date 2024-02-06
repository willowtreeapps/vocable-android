package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.utils.locale.LocalizedResourceUtility
import kotlinx.coroutines.*
import org.koin.core.component.inject

/**
 * A subclass of AppCompatRadioButton that represents a category on the main screen
 */
class CategoryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ActionButton(context, attrs, defStyle),
    PointerListener {

    private var buttonJob: Job? = null
    var category: Category? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    init {
        isEnabled = false
        setOnClickListener {
            isSelected = true
            sayText(text)
            performAction()
        }
    }

    fun setUpDisplayAndAction(
        category: Category,
        onCategorySelected: () -> Unit
    ) {
        this.category = category
        this.action = onCategorySelected
        this.text = localizedResourceUtility.getTextFromCategory(category)

    }

    override fun onPointerEnter() {
        if (isSelected) {
            return
        }
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isPressed = true
            }

            delay(dwellTime)

            uiScope.launch {
                isPressed = false
                isSelected = true
                sayText(text)
                performAction()
            }
        }
    }

    override fun onPointerExit() {
        isPressed = false
        buttonJob?.cancel()
    }

    override fun sayText(text: CharSequence?) {
        // No-op
    }
}