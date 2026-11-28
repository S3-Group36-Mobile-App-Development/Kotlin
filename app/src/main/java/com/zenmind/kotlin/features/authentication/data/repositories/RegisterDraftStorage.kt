package com.zenmind.kotlin.features.authentication.data.repositories

import android.content.Context

data class RegisterDraft(val name: String, val email: String)

class RegisterDraftStorage(context: Context) {
    private val preferences = context.getSharedPreferences("zenmind_register_draft", Context.MODE_PRIVATE)

    fun load(): RegisterDraft = RegisterDraft(
        name = preferences.getString("name", "").orEmpty(),
        email = preferences.getString("email", "").orEmpty()
    )

    fun save(name: String, email: String) {
        preferences.edit().putString("name", name).putString("email", email).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
