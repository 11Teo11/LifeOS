package com.example.lifeos.ui.calendar

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lifeos.ui.studybudget.CalendarState
import com.example.lifeos.ui.studybudget.CalendarViewModel

data class CalendarConnector(
    val state: CalendarState,
    val onConnectClick: () -> Unit,
    val onReset: () -> Unit,
)

@Composable
fun rememberCalendarConnector(viewModel: CalendarViewModel): CalendarConnector {
    val state by viewModel.calendarState.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val name = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (name != null) viewModel.syncCalendar(name)
        else viewModel.onAccountPickerDismissed()
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(state) {
        if (state is CalendarState.NeedsConsent) {
            val intent = (state as CalendarState.NeedsConsent).intent
            viewModel.onConsentLaunched()
            consentLauncher.launch(intent)
        }
    }

    return CalendarConnector(
        state = state,
        onConnectClick = { pickerLauncher.launch(viewModel.getAccountPickerIntent()) },
        onReset = viewModel::resetState
    )
}
