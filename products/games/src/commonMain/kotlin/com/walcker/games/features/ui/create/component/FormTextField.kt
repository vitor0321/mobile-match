package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
) {
    val supporting = error ?: helper

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        supportingText = supporting?.let { text -> { Text(text) } },
        isError = error != null,
        singleLine = true,
        shape = CedarTokens.radius.smShape,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
    )
}
