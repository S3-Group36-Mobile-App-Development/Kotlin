package com.zenmind.kotlin.features.panicbutton.data

import android.content.Context
import com.zenmind.kotlin.features.panicbutton.model.SupportContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepository (
    private val dao: ContactDao
) {
    fun getContacts(): Flow<List<SupportContact>> {
        return dao.getAllContacts().map { entities ->
            entities.map { entity ->
                SupportContact(
                    id = entity.id,
                    name = entity.name,
                    phone = entity.phone
                )
            }
        }
    }

    suspend fun save(contact: SupportContact) {
        val entity = ContactEntity (
            id = contact.id,
            name = contact.name,
            phone = contact.phone
        )
        dao.insert(entity)
    }
}