package com.example.lifeos.data.report

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.reportDataStore: DataStore<Preferences> by preferencesDataStore(name = "report_prefs")

class ReportPreferences(private val context: Context) {

    companion object {
        private val REPORT_TEXT = stringPreferencesKey("report_text")
        private val REPORT_DATE = stringPreferencesKey("report_date")
    }

    val reportText: Flow<String> = context.reportDataStore.data
        .map { it[REPORT_TEXT] ?: "" }

    val reportDate: Flow<String> = context.reportDataStore.data
        .map { it[REPORT_DATE] ?: "" }

    suspend fun saveReport(text: String, date: String) {
        context.reportDataStore.edit { prefs ->
            prefs[REPORT_TEXT] = text
            prefs[REPORT_DATE] = date
        }
    }
}