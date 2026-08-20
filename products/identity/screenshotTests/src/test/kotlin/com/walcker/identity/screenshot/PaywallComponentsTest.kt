@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.identity.screenshot

import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.identity.features.ui.paywall.components.EmptyPaywallState
import com.walcker.identity.features.ui.paywall.components.OfferingCard
import com.walcker.identity.features.ui.paywall.components.PaywallLegalFooter
import com.walcker.identity.features.ui.paywall.components.SubscriptionInfoSection
import com.walcker.identity.strings.LocalIdentityStrings
import org.junit.Rule
import org.junit.Test

class PaywallComponentsTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private val monthlyOffering = ProductOffering(
        id = "default:\$rc_monthly",
        offeringId = "default",
        packageId = "\$rc_monthly",
        title = "Plano mensal",
        description = "Apoie o Match e tenha acesso antecipado ao que vem.",
        priceLabel = "R\$ 14,90",
    )

    private val annualOffering = ProductOffering(
        id = "default:\$rc_annual",
        offeringId = "default",
        packageId = "\$rc_annual",
        title = "Plano anual",
        description = "Melhor custo-benefício para usar o Match todos os dias.",
        priceLabel = "R\$ 119,90",
    )

    @Test
    fun offeringCard_selected_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                val strings = LocalIdentityStrings.current.paywall
                OfferingCard(
                    offering = annualOffering,
                    isSelected = true,
                    selectedLabel = strings.selectedPlanLabel,
                    selectHint = strings.selectPlanHint,
                    colors = MaterialTheme.colorScheme,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun offeringCard_selected_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                val strings = LocalIdentityStrings.current.paywall
                OfferingCard(
                    offering = annualOffering,
                    isSelected = true,
                    selectedLabel = strings.selectedPlanLabel,
                    selectHint = strings.selectPlanHint,
                    colors = MaterialTheme.colorScheme,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun offeringCard_notSelected_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                val strings = LocalIdentityStrings.current.paywall
                OfferingCard(
                    offering = monthlyOffering,
                    isSelected = false,
                    selectedLabel = strings.selectedPlanLabel,
                    selectHint = strings.selectPlanHint,
                    colors = MaterialTheme.colorScheme,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun offeringCard_notSelected_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                val strings = LocalIdentityStrings.current.paywall
                OfferingCard(
                    offering = monthlyOffering,
                    isSelected = false,
                    selectedLabel = strings.selectedPlanLabel,
                    selectHint = strings.selectPlanHint,
                    colors = MaterialTheme.colorScheme,
                    onClick = {},
                )
            }
        }
    }

    @Test
    fun subscriptionInfoSection_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                val strings = LocalIdentityStrings.current.paywall
                SubscriptionInfoSection(
                    title = strings.subscriptionInfoTitle,
                    explanation = strings.billingExplanation,
                    colors = MaterialTheme.colorScheme,
                )
            }
        }
    }

    @Test
    fun subscriptionInfoSection_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                val strings = LocalIdentityStrings.current.paywall
                SubscriptionInfoSection(
                    title = strings.subscriptionInfoTitle,
                    explanation = strings.billingExplanation,
                    colors = MaterialTheme.colorScheme,
                )
            }
        }
    }

    @Test
    fun paywallLegalFooter_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                val strings = LocalIdentityStrings.current.paywall
                PaywallLegalFooter(
                    termsLabel = strings.termsOfUseLabel,
                    privacyLabel = strings.privacyPolicyLabel,
                    colors = MaterialTheme.colorScheme,
                    onOpenTerms = {},
                    onOpenPrivacy = {},
                )
            }
        }
    }

    @Test
    fun paywallLegalFooter_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                val strings = LocalIdentityStrings.current.paywall
                PaywallLegalFooter(
                    termsLabel = strings.termsOfUseLabel,
                    privacyLabel = strings.privacyPolicyLabel,
                    colors = MaterialTheme.colorScheme,
                    onOpenTerms = {},
                    onOpenPrivacy = {},
                )
            }
        }
    }

    @Test
    fun emptyPaywallState_lightMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = false) {
                val strings = LocalIdentityStrings.current.paywall
                EmptyPaywallState(
                    message = strings.emptyState,
                    retryLabel = strings.retryButton,
                    colors = MaterialTheme.colorScheme,
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun emptyPaywallState_darkMode() {
        paparazzi.snapshot {
            IdentitySnapshotTheme(darkTheme = true) {
                val strings = LocalIdentityStrings.current.paywall
                EmptyPaywallState(
                    message = strings.emptyState,
                    retryLabel = strings.retryButton,
                    colors = MaterialTheme.colorScheme,
                    onRetry = {},
                )
            }
        }
    }
}

