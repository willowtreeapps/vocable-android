package com.willowtree.vocable.room

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.koin.core.KoinComponent
import org.koin.core.inject

object Converters : KoinComponent {

    private val moshi: Moshi by inject()

    @TypeConverter
    @JvmStatic
    fun stringMapToJson(stringMap: Map<String, String>?): String {
        val type =
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
        return adapter.toJson(stringMap)
    }

    @TypeConverter
    @JvmStatic
    fun jsonToStringMap(json: String): Map<String, String>? {
        val type =
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val adapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)
        return adapter.fromJson(json)
    }
}