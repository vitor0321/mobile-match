package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val DEFAULT_HOUR = 19

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeSection(
    selectedDateMillis: Long?,
    selectedTime: Pair<Int, Int>?,
    dateError: String?,
    timeError: String?,
    strings: CreateMatchStrings,
    enabled: Boolean,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            CedarFilterRow(
                label = strings.dateLabel,
                value = selectedDateMillis?.let(::formatDate),
                placeholder = strings.datePlaceholder,
                onClick = { showDatePicker = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            CedarFilterRow(
                label = strings.timeLabel,
                value = selectedTime?.let { formatTime(it.first, it.second) },
                placeholder = strings.timePlaceholder,
                onClick = { showTimePicker = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }

        val error = dateError ?: timeError
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis,
            )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        showDatePicker = false
                    },
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = selectedTime?.first ?: DEFAULT_HOUR,
                initialMinute = selectedTime?.second ?: 0,
                is24Hour = true,
            )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = CedarTokens.radius.lgShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = CedarTokens.elevation.overlay,
            ) {
                Column(
                    modifier = Modifier.padding(CedarTokens.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(strings.cancel)
                        }
                        TextButton(
                            onClick = {
                                onTimeSelected(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            },
                        ) { Text(strings.confirm) }
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val pickedDate =
        Instant
            .fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.UTC)
            .date
    return "${pickedDate.dayOfMonth.toString().padStart(2, '0')}/" +
        "${pickedDate.monthNumber.toString().padStart(2, '0')}/" +
        "${pickedDate.year}"
}

private fun formatTime(
    hour: Int,
    minute: Int,
): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
