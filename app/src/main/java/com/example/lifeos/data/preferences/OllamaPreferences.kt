package com.example.lifeos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ollamaDataStore by preferencesDataStore(name = "ollama_prefs")

class OllamaPreferences(private val context: Context) {

    companion object {
        private val OLLAMA_HOST_KEY = stringPreferencesKey("ollama_host")
        const val DEFAULT_HOST = "10.0.2.2"
    }

    val ollamaHost: Flow<String> = context.ollamaDataStore.data
        .map { prefs -> prefs[OLLAMA_HOST_KEY] ?: DEFAULT_HOST }

    suspend fun setOllamaHost(host: String) {
        context.ollamaDataStore.edit { prefs ->
            prefs[OLLAMA_HOST_KEY] = host
        }
    }
}