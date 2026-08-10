package com.walcker.identity.features.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.core.strings.Locales

internal class ProfileStep : Screen {
    @Composable
    override fun Content() {
        WithIdentityStrings {
            val stepModel = koinScreenModel<ProfileStepModel>()
            val state by stepModel.state.collectAsState()
            val uriHandler = LocalUriHandler.current

            ProfileScreen(
                state = state,
                onBack = stepModel::onBackClicked,
                onUpgradeToPro = stepModel::onUpgradeToProClicked,
                onChangePlan = stepModel::onUpgradeToProClicked,
                onManageSubscription = { url -> uriHandler.openUri(url) },
                onRestorePurchases = stepModel::onRestorePurchasesClicked,
                onDeleteAccount = stepModel::onDeleteAccountClicked,
                onDeleteAccountConfirmationDismissed = stepModel::onDeleteAccountConfirmationDismissed,
                onDeleteAccountConfirmed = stepModel::onDeleteAccountConfirmed,
                onSignOut = stepModel::onSignOutClicked,
                onLanguageSelected = stepModel::onLanguageSelected,
            )
        }
    }
}

@Composable
internal fun ProfileScreen(
    state: ProfileState,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onChangePlan: () -> Unit = {},
    onManageSubscription: (String) -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onDeleteAccountConfirmationDismissed: () -> Unit = {},
    onDeleteAccountConfirmed: () -> Unit = {},
    onSignOut: () -> Unit,
    onLanguageSelected: (String) -> Unit = {},
) {
    val strings = LocalIdentityStrings.current.profile
    var showLanguageMenu by remember { mutableStateOf(false) }

    if (state.showDeleteAccountConfirmation) {
        AlertDialog(
            onDismissRequest = onDeleteAccountConfirmationDismissed,
            title = { Text(strings.deleteAccountConfirmationTitle) },
            text = { Text(strings.deleteAccountConfirmationMessage) },
            confirmButton = {
                Button(onClick = onDeleteAccountConfirmed) {
                    Text(strings.deleteAccountConfirmationButton)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDeleteAccountConfirmationDismissed) {
                    Text(strings.deleteAccountCancelButton)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CedarTopBar(
                title = strings.title,
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = state.userSession?.displayName ?: state.userSession?.email ?: strings.fallbackAccountName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            state.userSession?.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (state.isPro) strings.proStatusLabel else strings.freeStatusLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (state.isPro) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (state.isPro) strings.proStatusDescription else strings.freeStatusDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.isPro) {
                Button(
                    onClick = onUpgradeToPro,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && !state.isRestoringPurchases && !state.isDeletingAccount,
                ) {
                    Text(strings.subscribeProButton)
                }
            } else {
                Button(
                    onClick = onChangePlan,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && !state.isRestoringPurchases && !state.isDeletingAccount,
                ) {
                    Text(strings.changePlanButton)
                }
                OutlinedButton(
                    onClick = { state.managementUrl?.let(onManageSubscription) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.managementUrl != null &&
                        !state.isLoading &&
                        !state.isRestoringPurchases &&
                        !state.isDeletingAccount,
                ) {
                    Text(strings.manageSubscriptionButton)
                }
                if (state.managementUrl == null) {
                    Text(
                        text = strings.manageSubscriptionUnavailable,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Language Selection Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.languageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(
                    onClick = { showLanguageMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when (state.selectedLanguage) {
                            Locales.PT -> strings.languagePortuguese
                            Locales.EN -> strings.languageEnglish
                            else -> strings.languagePortuguese
                        }
                    )
                }
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.languagePortuguese) },
                        onClick = {
                            onLanguageSelected(Locales.PT)
                            showLanguageMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.languageEnglish) },
                        onClick = {
                            onLanguageSelected(Locales.EN)
                            showLanguageMenu = false
                        }
                    )
                }
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onRestorePurchases,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isRestoringPurchases && !state.isDeletingAccount,
            ) {
                Text(
                    if (state.isRestoringPurchases) {
                        strings.restorePurchasesLoadingButton
                    } else {
                        strings.restorePurchasesButton
                    },
                )
            }
            OutlinedButton(
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isRestoringPurchases && !state.isDeletingAccount,
            ) {
                Text(
                    if (state.isDeletingAccount) {
                        strings.deleteAccountLoadingButton
                    } else {
                        strings.deleteAccountButton
                    },
                )
            }
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                enabled = !state.isLoading && !state.isRestoringPurchases && !state.isDeletingAccount,
            ) {
                Text(if (state.isLoading) strings.signOutLoadingButton else strings.signOutButton)
            }
        }
    }
}
