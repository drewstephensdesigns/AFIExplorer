package com.drewcodesit.afiexplorer.utils.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * A Room Type Converter to handle the conversion between a List<Float>
 * (your vector embeddings) and a String (which SQLite can store).
 * Uses Gson for JSON serialization and deserialization.
 */
class FloatListConverter {

    private val gson = Gson()

    /**
     * Converts a List<Float> to a JSON String.
     * This string will be stored in the Room database.
     * @param value The List<Float> to convert.
     * @return The JSON String representation of the list, or null if the input is null.
     */
    @TypeConverter
    fun fromFloatList(value: List<Float>?): String? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.toJson(value, type)
    }

    /**
     * Converts a JSON String back to a List<Float>.
     * This is used when reading data from the Room database.
     * @param value The JSON String to convert.
     * @return The List<Float> representation of the string, or null if the input is null.
     */

    @TypeConverter
    fun toFloatList(value: String?): List<Float>? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(value, type)
    }
}