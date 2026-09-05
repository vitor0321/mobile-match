package com.walcker.games.features.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.games.strings.AboutStrings
import com.walcker.games.strings.rememberGamesStrings
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject

private const val TERMS_URL = "https://vitor0321.github.io/join-play-privacy-policy.html"
private const val CONTACT_EMAIL = "vitorwalcker.dev@gmail.com"

internal class AboutStep : Screen {
    @Composable
    override fun Content() {
        val strings = rememberGamesStrings().strings.about
        val navigator = LocalNavigator.currentOrThrow
        val bottomBarVisibility: BottomBarVisibilityCoordinator = koinInject()

        DisposableEffect(bottomBarVisibility) {
            bottomBarVisibility.setVisible(false)
            onDispose { bottomBarVisibility.setVisible(true) }
        }

        AboutContent(
            strings = strings,
            onBack = { navigator.pop() },
        )
    }
}

@Composable
internal fun AboutContent(
    strings: AboutStrings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier,
        containerColor = CedarTokens.colors.canvas,
        topBar = {
            CedarTopBar(
                title = strings.title,
                onBack = onBack,
                backContentDescription = strings.backContentDescription,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CedarTokens.spacing.lg, vertical = CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.lg),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = strings.appTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            AboutSectionCard(title = strings.storyTitle, body = strings.storyBody)
            AboutSectionCard(title = strings.missionTitle, body = strings.missionBody)

            CedarTextButton(
                text = strings.termsLabel,
                onClick = { uriHandler.openUri(TERMS_URL) },
            )

            Text(
                text = "${strings.contactLabel}: $CONTACT_EMAIL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = CedarTokens.spacing.md)
                        .clickable { uriHandler.openUri("mailto:$CONTACT_EMAIL") },
            )
        }
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
