package com.zenmind.kotlin.features.protocols.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.zenmind.kotlin.features.protocols.model.ProtocolStep

class StepsConverter {
    private val gson = Gson()

    @TypeConverter
    fun toJson(steps: List<ProtocolStep>): String = gson.toJson(steps)

    @TypeConverter
    fun fromJson(json: String): List<ProtocolStep> =
        gson.fromJson(json, Array<ProtocolStep>::class.java).toList()
}
