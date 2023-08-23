package com.willowtree.vocable

import android.app.Application
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
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
//                            .setFontAttrId(R.attr.fontPath)
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