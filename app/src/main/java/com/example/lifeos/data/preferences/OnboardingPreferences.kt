package com.example.lifeos.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferences(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ONBOARDING_FULLY_COMPLETED = booleanPreferencesKey("onboarding_fully_completed")
        val USER_NAME = stringPreferencesKey("user_name")
        val MONTHLY_BUDGET = stringPreferencesKey("monthly_budget")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_COMPLETED] ?: false }

    val isOnboardingFullyCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_FULLY_COMPLETED] ?: false }

    val userName: Flow<String> = context.dataStore.data
        .map { it[USER_NAME] ?: "" }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    suspend fun setOnboardingFullyCompleted() {
        context.dataStore.edit { it[ONBOARDING_FULLY_COMPLETED] = true }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun saveMonthlyBudget(budget: String) {
        context.dataStore.edit { it[MONTHLY_BUDGET] = budget }
    }
}