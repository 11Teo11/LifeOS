package com.example.lifeos.ui.habit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeos.data.agent.PlanSuggestion

@Composable
fun DayPlanCard(
    state: DayPlanState,
    onGenerate: () -> Unit,
    onUpdateSuggestion: (Int, String, String) -> Unit,
    onRemoveSuggestion: (Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tomorrow's Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            when (state) {
                is DayPlanState.Idle -> IdleContent(onGenerate)
                is DayPlanState.InsufficientData -> InsufficientContent(state)
                is DayPlanState.Loading -> LoadingContent()
                is DayPlanState.Draft -> DraftContent(
                    state, onUpdateSuggestion, onRemoveSuggestion, onSave, onDiscard
                )
                is DayPlanState.Saved -> SavedContent(state, onRegenerate)
                is DayPlanState.Error -> ErrorContent(state, onGenerate)
            }
        }
    }
}

@Composable
private fun IdleContent(onGenerate: () -> Unit) {
    Text(
        text = "Get a personalized set of suggestions for tomorrow based on your wellness, habits, budget, and academic context.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
        Text("Generate tomorrow's plan")
    }
}

@Composable
private fun InsufficientContent(state: DayPlanState.InsufficientData) {
    Text(
        text = "Log at least ${state.checkInsRequired} daily check-ins to unlock this. " +
            "You have ${state.checkInsAvailable} so far.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun LoadingContent() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Generating plan…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DraftContent(
    state: DayPlanState.Draft,
    onUpdateSuggestion: (Int, String, String) -> Unit,
    onRemoveSuggestion: (Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Text(
        text = "Edit before saving. Source: ${state.source}. " +
            "Energy avg: ${"%.1f".format(state.energyAvg)}/10.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))

    state.suggestions.forEachIndexed { index, suggestion ->
        EditableSuggestionRow(
            index = index,
            suggestion = suggestion,
            onChange = { s, j -> onUpdateSuggestion(index, s, j) },
            onRemove = { onRemoveSuggestion(index) }
        )
        if (index < state.suggestions.lastIndex) Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(12.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
            Text("Discard")
        }
        Button(
            onClick = onSave,
            enabled = state.suggestions.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) {
            Text("Save plan")
        }
    }
}

@Composable
private fun EditableSuggestionRow(
    index: Int,
    suggestion: PlanSuggestion,
    onChange: (String, String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                CategoryChip(suggestion.category)
                PriorityChip(suggestion.priority)
                EffortChip(suggestion.effort)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text("Remove") }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = suggestion.suggestion,
                onValueChange = { onChange(it, suggestion.justification) },
                label = { Text("Suggestion") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = suggestion.justification,
                onValueChange = { onChange(suggestion.suggestion, it) },
                label = { Text("Why") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun SavedContent(state: DayPlanState.Saved, onRegenerate: () -> Unit) {
    Text(
        text = "Saved for ${state.targetDate}. Source: ${state.source}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    state.suggestions.forEachIndexed { idx, s ->
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "#${idx + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    CategoryChip(s.category)
                    PriorityChip(s.priority)
                    EffortChip(s.effort)
                }
                Spacer(Modifier.height(4.dp))
                Text(s.suggestion, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    s.justification,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (idx < state.suggestions.lastIndex) Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onRegenerate, modifier = Modifier.fillMaxWidth()) {
        Text("Regenerate")
    }
}

@Composable
private fun ErrorContent(state: DayPlanState.Error, onRetry: () -> Unit) {
    Text(
        text = state.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Try again")
    }
}

@Composable
private fun CategoryChip(category: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(category) }
    )
}

@Composable
private fun PriorityChip(priority: String) {
    val color = when (priority) {
        "high" -> MaterialTheme.colorScheme.errorContainer
        "low" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            text = priority,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun EffortChip(effort: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = "effort: $effort",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
