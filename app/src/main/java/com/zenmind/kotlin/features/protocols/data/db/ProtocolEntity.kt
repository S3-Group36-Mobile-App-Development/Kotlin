package com.zenmind.kotlin.features.protocols.data.db

import androidx.room.Entity
import com.zenmind.kotlin.features.protocols.model.ProtocolStep

@Entity(tableName = "protocol_table", primaryKeys = ["id", "userId"])
data class ProtocolEntity(
    val id: Long,
    val userId: String,
    val titulo: String,
    val esPredefinido: Boolean,
    val pasos: List<ProtocolStep>,
    val synced: Boolean
)
