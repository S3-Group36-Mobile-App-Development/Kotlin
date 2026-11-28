package com.zenmind.kotlin.features.protocols.model

data class Protocol(
    val id: Long,
    val titulo: String,
    val esPredefinido: Boolean,
    val pasos: List<ProtocolStep>,
    val synced: Boolean = true
)

data class ProtocolStep(
    val ordenPaso: Int,
    val descripcion: String
)
