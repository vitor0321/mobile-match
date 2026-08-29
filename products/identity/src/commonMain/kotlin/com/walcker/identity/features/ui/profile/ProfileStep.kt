package com.walcker.identity.features.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.ProfileStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarFilterRow
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.PlayerAvatar
import com.walcker.match.cedar.components.PlayerAvatarSize
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.strings.Locales

internal class ProfileStep : Screen {

    override val key: String get() = "profile-settings"

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
                onDeleteAccountConfirmationDismissed =
                    stepModel::onDeleteAccountConfirmationDismissed,
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

    val isBusy = state.isLoading || state.isRestoringPurchases || state.isDeletingAccount

    if (state.showDeleteAccountConfirmation) {
        DeleteAccountDialog(
            strings = strings,
            isDeleting = state.isDeletingAccount,
            onConfirm = onDeleteAccountConfirmed,
            onDismiss = onDeleteAccountConfirmationDismissed,
        )
    }

    Scaffold(
        topBar = {
            CedarTopBar(
                title = strings.title,
                onBack = onBack,
                backContentDescription = strings.back,
            )
        },
        containerColor = CedarTokens.colors.canvas,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
        ) {
            AccountHeader(
                displayName = state.userSession?.displayName
                    ?: state.userSession?.email
                    ?: strings.fallbackAccountName,
                email = state.userSession?.email,
            )

            PlanCard(
                isPro = state.isPro,
                managementUrl = state.managementUrl,
                strings = strings,
                enabled = !isBusy,
                onUpgradeToPro = onUpgradeToPro,
                onChangePlan = onChangePlan,
                onManageSubscription = onManageSubscription,
            )

            LanguagePicker(
                selectedLanguage = state.selectedLanguage,
                strings = strings,
                enabled = !isBusy,
                onLanguageSelected = onLanguageSelected,
            )

            state.message?.let { message ->
                AuthFormMessage(text = message, isError = false)
            }
            state.error?.let { error ->
                AuthFormMessage(text = error, isError = true)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CedarSectionHeader(title = strings.accountActionsLabel)

            CedarSecondaryButton(
                text = strings.restorePurchasesButton,
                onClick = onRestorePurchases,
                enabled = !isBusy,
                loading = state.isRestoringPurchases,
            )

            CedarSecondaryButton(
                text = strings.signOutButton,
                onClick = onSignOut,
                enabled = !isBusy,
                loading = state.isLoading,
            )

            TextButton(
                onClick = onDeleteAccount,
                enabled = !isBusy,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (state.isDeletingAccount) {
                        strings.deleteAccountLoadingButton
                    } else {
                        strings.deleteAccountButton
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountHeader(
    displayName: String,
    email: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        PlayerAvatar(
            displayName = displayName,
            size = PlayerAvatarSize.Large,
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (email != null && email != displayName) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlanCard(
    isPro: Boolean,
    managementUrl: String?,
    strings: ProfileStrings,
    enabled: Boolean,
    onUpgradeToPro: () -> Unit,
    onChangePlan: () -> Unit,
    onManageSubscription: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CedarTokens.radius.mdShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Text(
                text = if (isPro) strings.proStatusLabel else strings.freeStatusLabel,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPro) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = if (isPro) strings.proStatusDescription else strings.freeStatusDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isPro) {
                CedarPrimaryButton(
                    text = strings.changePlanButton,
                    onClick = onChangePlan,
                    enabled = enabled,
                )
                CedarSecondaryButton(
                    text = strings.manageSubscriptionButton,
                    onClick = { managementUrl?.let(onManageSubscription) },
                    enabled = enabled && managementUrl != null,
                )
                if (managementUrl == null) {
                    Text(
                        text = strings.manageSubscriptionUnavailable,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                CedarPrimaryButton(
                    text = strings.subscribeProButton,
                    onClick = onUpgradeToPro,
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun LanguagePicker(
    selectedLanguage: String,
    strings: ProfileStrings,
    enabled: Boolean,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        CedarFilterRow(
            label = strings.languageLabel,
            value = strings.labelForLanguage(selectedLanguage),
            placeholder = strings.languageLabel,
            onClick = { expanded = true },
            enabled = enabled,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(strings.languagePortuguese) },
                onClick = {
                    onLanguageSelected(Locales.PT)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(strings.languageEnglish) },
                onClick = {
                    onLanguageSelected(Locales.EN)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    strings: ProfileStrings,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CedarTokens.radius.lgShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(strings.deleteAccountConfirmationTitle) },
        text = { Text(strings.deleteAccountConfirmationMessage) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(strings.deleteAccountConfirmationButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(strings.deleteAccountCancelButton)
            }
        },
    )
}

private fun ProfileStrings.labelForLanguage(tag: String): String = when (tag) {
    Locales.EN -> languageEnglish
    else -> languagePortuguese
}
