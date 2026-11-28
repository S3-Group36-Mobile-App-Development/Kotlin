package com.zenmind.kotlin.features.panic.data

import com.zenmind.kotlin.features.panic.data.db.EmergencyContactDao
import com.zenmind.kotlin.features.panic.data.db.toContact
import com.zenmind.kotlin.features.panic.data.db.toEntity
import com.zenmind.kotlin.features.panic.model.EmergencyContact

class PanicRepository(
    private val dao: EmergencyContactDao
) {
    private var cached: EmergencyContact? = null
    private var loaded = false

    suspend fun getContact(): EmergencyContact? {
        if (loaded) return cached
        cached = dao.get()?.toContact()
        loaded = true
        return cached
    }

    suspend fun saveContact(contact: EmergencyContact) {
        dao.save(contact.toEntity())
        cached = contact
        loaded = true
    }

    suspend fun deleteContact() {
        dao.delete()
        cached = null
        loaded = true
    }
}
