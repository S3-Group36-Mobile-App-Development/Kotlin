package com.zenmind.kotlin.features.dailycheckin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.draftDataStore by preferencesDataStore(name = "checkin_draft")

class CheckInDraftStore(private val context: Context) {

    suspend fun read(userId: String): String =
        context.draftDataStore.data.first()[keyFor(userId)] ?: ""

    suspend fun save(userId: String, note: String) {
        context.draftDataStore.edit { prefs ->
            prefs[keyFor(userId)] = note
        }
    }

    private fun keyFor(userId: String) = stringPreferencesKey("note_$userId")
}
