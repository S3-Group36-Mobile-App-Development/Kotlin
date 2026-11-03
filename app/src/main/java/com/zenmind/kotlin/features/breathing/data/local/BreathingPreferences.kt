package com.zenmind.kotlin.features.breathing.data.local

import android.content.Context

class BreathingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences (
        "zenmind_breathing_prefs",
        Context.MODE_PRIVATE
    )

    fun setFavorite(ejercicioId: Int) {
        prefs.edit().putInt(KEY_FAVORITE, ejercicioId).apply()
    }

    fun getFavorite(): Int? {
        val value = prefs.getInt(KEY_FAVORITE, NO_FAVORTIE)
        return if (value == NO_FAVORTIE) null else value
    }

    fun clearFavorite() {
        prefs.edit().remove(KEY_FAVORITE).apply()
    }

    private companion object {
        const val KEY_FAVORITE = "favorite_exercise_id"
        const val NO_FAVORTIE = -1
    }
}