package com.zenmind.kotlin.features.panic.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zenmind.kotlin.features.panic.model.EmergencyContact

@Entity(tableName = "emergency_contact_table")
data class EmergencyContactEntity(
    @PrimaryKey val id: Int = SINGLE_ID,
    val nombre: String,
    val telefono: String,
    val relacion: String?
) {
    companion object {
        const val SINGLE_ID = 1
    }
}

fun EmergencyContactEntity.toContact() = EmergencyContact(nombre, telefono, relacion)

fun EmergencyContact.toEntity() = EmergencyContactEntity(nombre = nombre, telefono = telefono, relacion = relacion)
