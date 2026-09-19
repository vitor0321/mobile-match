package com.walcker.identity.features.ui.verification.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.walcker.identity.features.data.platform.localizedCountryName
import com.walcker.identity.features.domain.phone.countryDialCodes
import com.walcker.identity.features.domain.phone.digitsOnly
import com.walcker.identity.features.domain.phone.flagEmoji
import com.walcker.identity.features.domain.phone.foldAccents
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.match.cedar.tokens.CedarTokens

private val CountryRowMinHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CountryPickerSheet(
    selectedIsoCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalIdentityStrings.current.verification
    var query by remember { mutableStateOf("") }
    val countries =
        remember {
            countryDialCodes
                .map { country -> country to localizedCountryName(country.isoCode) }
                .sortedBy { (_, name) -> foldAccents(name) }
        }
    val filtered =
        remember(query, countries) {
            val trimmed = query.trim()
            val foldedQuery = foldAccents(trimmed)
            val queryDigits = digitsOnly(trimmed)
            if (trimmed.isEmpty()) {
                countries
            } else {
                countries.filter { (country, name) ->
                    foldAccents(name).contains(foldedQuery) ||
                        country.isoCode.equals(trimmed, ignoreCase = true) ||
                        (queryDigits.isNotEmpty() && country.dialCode.startsWith(queryDigits))
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CedarTokens.colors.canvas,
    ) {
        Text(
            text = strings.countryPickerTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = CedarTokens.spacing.lg),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(strings.countrySearchLabel) },
            singleLine = true,
            shape = CedarTokens.radius.smShape,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.sm),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            items(items = filtered, key = { (country, _) -> country.isoCode }) { (country, name) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = CountryRowMinHeight)
                            .selectable(
                                selected = country.isoCode == selectedIsoCode,
                                onClick = { onSelect(country.isoCode) },
                                role = Role.RadioButton,
                            ).padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                ) {
                    Text(
                        text = flagEmoji(country.isoCode),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "+${country.dialCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
