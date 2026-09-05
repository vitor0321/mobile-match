package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.walcker.games.features.ui.create.CreateMatchState
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.match.cedar.components.CedarFilterRow

@Composable
internal fun DurationPicker(
    selectedDurationMin: Int,
    strings: CreateMatchStrings,
    enabled: Boolean,
    onDurationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        CedarFilterRow(
            label = strings.durationLabel,
            value = strings.durationValue(selectedDurationMin),
            placeholder = strings.durationLabel,
            onClick = { expanded = true },
            enabled = enabled,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CreateMatchState.AVAILABLE_DURATIONS.forEach { duration ->
                DropdownMenuItem(
                    text = { Text(strings.durationValue(duration)) },
                    onClick = {
                        onDurationSelected(duration)
                        expanded = false
                    },
                )
            }
        }
    }
}
