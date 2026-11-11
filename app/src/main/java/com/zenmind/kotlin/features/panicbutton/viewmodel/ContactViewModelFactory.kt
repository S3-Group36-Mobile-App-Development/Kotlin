package com.zenmind.kotlin.features.panicbutton.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenmind.kotlin.core.database.AppDatabase
import com.zenmind.kotlin.features.panicbutton.data.ContactRepository

class ContactViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)

        val repository = ContactRepository (
            dao = db.panicButtonDao()
        )

        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(ContactViewModel::class.java) ->
                ContactViewModel(repository) as T

            else -> throw IllegalArgumentException("Unknow ViewModel: ${modelClass.name}")
        }
    }
}