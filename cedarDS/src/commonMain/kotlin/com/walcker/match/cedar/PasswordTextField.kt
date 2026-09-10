package com.walcker.match.cedar

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
public fun PasswordOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    showPasswordLabel: String,
    hidePasswordLabel: String,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        supportingText = supportingText?.let { text -> { Text(text) } },
        isError = isError,
        singleLine = singleLine,
        enabled = enabled,
        shape = CedarTokens.radius.smShape,
        keyboardOptions = keyboardOptions,
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector =
                        if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                    contentDescription =
                        if (passwordVisible) {
                            hidePasswordLabel
                        } else {
                            showPasswordLabel
                        },
                )
            }
        },
    )
}
