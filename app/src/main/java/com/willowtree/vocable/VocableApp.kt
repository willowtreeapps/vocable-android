package com.willowtree.vocable

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.View
import android.widget.TextView
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils
import io.github.inflationx.viewpump.ViewPump
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class VocableApp : Application() {

    override fun onCreate() {
        super.onCreate()

        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/Roboto-RobotoRegular.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )

        startKoin {
            androidContext(this@VocableApp)

            modules(listOf(AppKoinModule.getModule()))
        }
    }
}