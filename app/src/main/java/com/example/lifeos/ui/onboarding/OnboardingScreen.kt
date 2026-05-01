package com.example.lifeos.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                viewModel.skipOnboarding()
                onOnboardingComplete()
            }) {
                Text("Skip")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            repeat(3) { index ->
                val isActive = index == currentStep
                val isPast = index < currentStep
                Surface(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isActive) 32.dp else 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (isActive || isPast)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
        }

        when (currentStep) {
            0 -> StepProfile(
                userName = userName,
                onNameChange = { viewModel.setUserName(it) }
            )
            1 -> StepCalendar()
            2 -> StepBudget(
                budget = monthlyBudget,
                onBudgetChange = { viewModel.setMonthlyBudget(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = { viewModel.previousStep() }) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(onClick = {
                if (currentStep < 2) {
                    viewModel.nextStep()
                } else {
                    viewModel.completeOnboarding()
                    onOnboardingComplete()
                }
            }) {
                Text(if (currentStep < 2) "Continue" else "Let's go!")
            }
        }
    }
}

@Composable
fun StepProfile(userName: String, onNameChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step 1 of 3",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StepCalendar() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Connect Google Calendar",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step 2 of 3",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "By connecting your calendar, LifeOS agents will know when you have exams and deadlines.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = { /* Teo implements OAuth here */ }) {
            Text("Connect Google Calendar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {}) {
            Text("I'll do this later")
        }
    }
}

@Composable
fun StepBudget(budget: String, onBudgetChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What's your monthly budget?",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step 3 of 3",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = budget,
            onValueChange = onBudgetChange,
            label = { Text("Monthly budget (RON)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}