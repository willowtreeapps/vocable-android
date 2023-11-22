package com.willowtree.vocable.room

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.willowtree.vocable.utils.locale.LocaleString
import com.willowtree.vocable.utils.locale.LocalesWithText
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object Converters : KoinComponent {

    private val moshi: Moshi by inject()

    @TypeConverter
    @JvmStatic
    fun stringMapToJson(stringMap: Map<String, String>?): String {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
        return adapter.toJson(stringMap)
    }

    @TypeConverter
    @JvmStatic
    fun jsonToStringMap(json: String?): Map<String, String>? {
        return json?.let {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
            adapter.fromJson(it)
        }
    }

    @TypeConverter
    @JvmStatic
    fun stringMapToLanguagesWithText(localesWithText: LocalesWithText): String {
        val type = Types.newParameterizedType(Map::class.java, LocaleString::class.java, String::class.java)
        val adapter: JsonAdapter<Map<LocaleString, String>> = moshi.adapter(type)
        return adapter.toJson(localesWithText.localesTextMap)
    }

    @TypeConverter
    @JvmStatic
    fun languagesWithTextToStringMap(json: String?): LocalesWithText? {
        return json?.let {
            val type = Types.newParameterizedType(Map::class.java, LocaleString::class.java, String::class.java)
            val adapter: JsonAdapter<Map<LocaleString, String>> = moshi.adapter(type)
            adapter.fromJson(it)?.let { stringMap ->
                LocalesWithText(stringMap)
            }

        }
    }
}