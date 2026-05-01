package com.example.lifeos.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeos.data.onboarding.OnboardingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(context: Context) : ViewModel() {

    private val prefs = OnboardingPreferences(context)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _monthlyBudget = MutableStateFlow("")
    val monthlyBudget: StateFlow<String> = _monthlyBudget.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.isOnboardingCompleted.collect {
                _isCompleted.value = it
            }
        }
    }

    fun setUserName(name: String) { _userName.value = name }

    fun setMonthlyBudget(budget: String) { _monthlyBudget.value = budget }

    fun nextStep() {
        if (_currentStep.value < 2) _currentStep.value++
    }

    fun previousStep() {
        if (_currentStep.value > 0) _currentStep.value--
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingCompleted()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.saveUserName(_userName.value)
            prefs.saveMonthlyBudget(_monthlyBudget.value)
            prefs.setOnboardingCompleted()
        }
    }
}