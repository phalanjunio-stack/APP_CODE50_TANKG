package com.srlakes.tone.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun floatListToString(value: List<Float>?): String =
        value?.joinToString(separator = ",") { it.toString() } ?: ""

    @TypeConverter
    fun stringToFloatList(value: String?): List<Float> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toFloatOrNull() }
    }
}
