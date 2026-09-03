package com.walcker.games.features.ui.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.strings.SearchStrings
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarFilterSection
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.components.SportChip
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.payments.currentDeviceCurrencyCode
import com.walcker.match.core.payments.formatCurrencyCents
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchFiltersPanel(
    strings: SearchStrings,
    selectedSports: Set<Sport>,
    startDateMs: Long?,
    endDateMs: Long?,
    minPrice: Float?,
    maxPrice: Float?,
    onSportToggled: (Sport?) -> Unit,
    onDateRangeChanged: (startDateMs: Long?, endDateMs: Long?) -> Unit,
    onPriceRangeChanged: (minPrice: Float?, maxPrice: Float?) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showMinPriceDialog by remember { mutableStateOf(false) }
    var showMaxPriceDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
    ) {
        Text(
            text = strings.filtersTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        CedarFilterSection(label = strings.filterSport) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs)) {
                item {
                    SportChip(
                        label = strings.allSports,
                        selected = selectedSports.isEmpty(),
                        onClick = { onSportToggled(null) },
                    )
                }
                items(items = Sport.entries) { sport ->
                    SportChip(
                        label = sport.label,
                        selected = sport in selectedSports,
                        onClick = { onSportToggled(sport) },
                    )
                }
            }
        }

        CedarFilterSection(label = strings.filterDate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                CedarFilterRow(
                    label = strings.dateFromLabel,
                    value = startDateMs?.let(::formatFilterDate),
                    placeholder = strings.filterAny,
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                )
                CedarFilterRow(
                    label = strings.dateToLabel,
                    value = endDateMs?.let(::formatFilterDate),
                    placeholder = strings.filterAny,
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CedarFilterSection(label = strings.filterPrice) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            ) {
                CedarFilterRow(
                    label = strings.priceMinLabel,
                    value = minPrice?.let(::formatFilterPrice),
                    placeholder = strings.filterAny,
                    onClick = { showMinPriceDialog = true },
                    modifier = Modifier.weight(1f),
                )
                CedarFilterRow(
                    label = strings.priceMaxLabel,
                    value = maxPrice?.let(::formatFilterPrice),
                    placeholder = strings.filterAny,
                    onClick = { showMaxPriceDialog = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CedarPrimaryButton(
            text = strings.applyFilters,
            onClick = onDismiss,
        )
        CedarTextButton(
            text = strings.clearFilters,
            onClick = onResetFilters,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMs)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeChanged(datePickerState.selectedDateMillis, endDateMs)
                        showStartDatePicker = false
                    },
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDateMs)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateRangeChanged(startDateMs, datePickerState.selectedDateMillis)
                        showEndDatePicker = false
                    },
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showMinPriceDialog) {
        PriceInputDialog(
            title = strings.priceMinLabel,
            initialValue = minPrice,
            confirmLabel = strings.confirm,
            cancelLabel = strings.cancel,
            onConfirm = { value ->
                onPriceRangeChanged(value, maxPrice)
                showMinPriceDialog = false
            },
            onDismiss = { showMinPriceDialog = false },
        )
    }

    if (showMaxPriceDialog) {
        PriceInputDialog(
            title = strings.priceMaxLabel,
            initialValue = maxPrice,
            confirmLabel = strings.confirm,
            cancelLabel = strings.cancel,
            onConfirm = { value ->
                onPriceRangeChanged(minPrice, value)
                showMaxPriceDialog = false
            },
            onDismiss = { showMaxPriceDialog = false },
        )
    }
}

@Composable
private fun PriceInputDialog(
    title: String,
    initialValue: Float?,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (Float?) -> Unit,
    onDismiss: () -> Unit,
) {
    // A fonte da verdade é o texto digitado, não o Float — texto intermediário como
    // "25," não parseia e não pode apagar o que a pessoa já escreveu.
    var text by remember { mutableStateOf(initialValue?.let { formatPriceInputValue(it) } ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CedarTokens.radius.lgShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = CedarTokens.elevation.overlay,
        ) {
            Column(
                modifier = Modifier.padding(CedarTokens.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = CedarTokens.radius.smShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(cancelLabel)
                    }
                    TextButton(
                        onClick = {
                            onConfirm(text.trim().replace(',', '.').toFloatOrNull())
                        },
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}

private fun formatFilterDate(millis: Long): String {
    val date =
        Instant
            .fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.UTC)
            .date
    return "${date.dayOfMonth.toString().padStart(2, '0')}/" +
        "${date.monthNumber.toString().padStart(2, '0')}/" +
        "${date.year}"
}

private fun formatFilterPrice(price: Float): String =
    formatCurrencyCents((price * 100).roundToInt(), currentDeviceCurrencyCode())

private fun formatPriceInputValue(price: Float): String =
    if (price == price.toInt().toFloat()) price.toInt().toString() else price.toString()
