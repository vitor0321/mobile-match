package com.walcker.identity.features.ui.paywall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.features.ui.paywall.components.EmptyPaywallState
import com.walcker.identity.features.ui.paywall.components.OfferingCard
import com.walcker.identity.features.ui.paywall.components.PaywallLegalFooter
import com.walcker.identity.features.ui.paywall.components.SubscriptionInfoSection
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarLoadingIndicator
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.IdentityDestination
import org.koin.compose.koinInject

private const val TERMS_OF_USE_URL = "https://vitor0321.github.io/terms-of-use.html"
private const val PRIVACY_POLICY_URL = "https://vitor0321.github.io/match-privacy-policy.html"

internal class PaywallStep : Screen {
    @Composable
    override fun Content() {
        WithIdentityStrings {
            val stepModel = koinScreenModel<PaywallStepModel>()
            val state by stepModel.state.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val navigatorHolder = koinInject<NavigatorHolder>()
            val identityDestination = koinInject<IdentityDestination>()
            val uriHandler = LocalUriHandler.current

            PaywallEvents(
                stepModel = stepModel,
                onDismiss = { navigatorHolder.navigator?.pop() },
                onRequireLogin = { navigatorHolder.navigator?.replace(identityDestination.login()) },
                onShowSnackbar = { message -> snackbarHostState.showSnackbar(message) },
            ) { onEvent ->
                PaywallScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onBack = { onEvent(PaywallInternalRoute.OnBackClicked) },
                    onRetry = { onEvent(PaywallInternalRoute.OnRetryClicked) },
                    onRestore = { onEvent(PaywallInternalRoute.OnRestoreClicked) },
                    onPurchase = { onEvent(PaywallInternalRoute.OnPurchaseClicked) },
                    onOfferingSelected = { onEvent(PaywallInternalRoute.OnOfferingSelected(it)) },
                    onManageSubscription = { url -> uriHandler.openUri(url) },
                    onOpenTerms = { uriHandler.openUri(TERMS_OF_USE_URL) },
                    onOpenPrivacy = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
            }
        }
    }
}

@Composable
internal fun PaywallScreen(
    state: PaywallState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    onRestore: () -> Unit = {},
    onPurchase: () -> Unit = {},
    onOfferingSelected: (String) -> Unit = {},
    onManageSubscription: (String) -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    PaywallScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRetry = onRetry,
        onRestore = onRestore,
        onPurchase = onPurchase,
        onOfferingSelected = onOfferingSelected,
        onManageSubscription = onManageSubscription,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
    )
}

@Composable
internal fun PaywallScreen(
    state: PaywallState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    onRestore: () -> Unit = {},
    onPurchase: () -> Unit = {},
    onOfferingSelected: (String) -> Unit = {},
    onManageSubscription: (String) -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
) {
    val strings = LocalIdentityStrings.current.paywall
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { CedarTopBar(title = strings.title, onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (state.isPro) strings.proHeadline else strings.headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (state.isPro) colors.primary else colors.onSurface,
            )
            Text(
                text = if (state.isPro) strings.proDescription else strings.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CedarLoadingIndicator(modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = strings.loadingLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }

                state.offerings.isEmpty() -> {
                    EmptyPaywallState(
                        message = state.errorMessage ?: strings.emptyState,
                        retryLabel = strings.retryButton,
                        colors = colors,
                        onRetry = onRetry,
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.offerings.forEach { offering ->
                            OfferingCard(
                                offering = offering,
                                isSelected = state.selectedOfferingId == offering.id,
                                selectedLabel = strings.selectedPlanLabel,
                                selectHint = strings.selectPlanHint,
                                colors = colors,
                                onClick = { onOfferingSelected(offering.id) },
                            )
                        }
                    }
                }
            }

            state.errorMessage?.takeIf { state.offerings.isNotEmpty() }?.let {
                Text(text = it, color = colors.error, style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(color = colors.outline)

            if (state.isPro) {
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.purchaseInProgress == null && state.selectedOfferingId != null,
                ) {
                    Text(if (state.purchaseInProgress != null) strings.purchaseLoadingButton else strings.changePlanButton)
                }
                OutlinedButton(
                    onClick = { state.managementUrl?.let(onManageSubscription) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.managementUrl != null && !state.isLoading && state.purchaseInProgress == null,
                ) {
                    Text(strings.manageSubscriptionButton)
                }
                if (state.managementUrl == null && !state.isLoading) {
                    Text(
                        text = strings.manageSubscriptionUnavailable,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading && state.purchaseInProgress == null && !state.isRestoring,
                    ) {
                        Text(if (state.isRestoring) strings.restoreLoadingButton else strings.restoreButton)
                    }
                    Button(
                        onClick = onPurchase,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading && state.purchaseInProgress == null && !state.isRestoring && state.selectedOfferingId != null,
                    ) {
                        Text(if (state.purchaseInProgress != null) strings.purchaseLoadingButton else strings.purchaseButton)
                    }
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.purchaseInProgress == null && !state.isRestoring,
            ) {
                Text(strings.backButton)
            }

            if (state.selectedOffering != null) {
                SubscriptionInfoSection(
                    title = strings.subscriptionInfoTitle,
                    explanation = strings.billingExplanation,
                    colors = colors,
                )
            }

            PaywallLegalFooter(
                termsLabel = strings.termsOfUseLabel,
                privacyLabel = strings.privacyPolicyLabel,
                colors = colors,
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
            )
        }
    }
}
