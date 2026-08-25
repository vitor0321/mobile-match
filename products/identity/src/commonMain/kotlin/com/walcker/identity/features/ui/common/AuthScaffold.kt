package com.walcker.identity.features.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.walcker.match.cedar.CedarTopBar
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * Moldura comum de entrar, criar conta e recuperar senha.
 *
 * As três telas eram o mesmo `Scaffold` + `Column` copiado, com os mesmos números
 * soltos (24.dp, 16.dp, 12.dp) escritos três vezes. Agora o espaçamento vem dos
 * tokens e mora num lugar só.
 *
 * Duas coisas que faltavam nas três:
 * - **Rolagem.** Num aparelho baixo, com o teclado aberto, o campo de senha e o
 *   botão ficavam fora da tela sem jeito de chegar até eles. [verticalScroll] mais
 *   [imePadding] resolvem.
 * - **Fundo.** As telas usavam `colorScheme.background`, que é branco puro; o resto
 *   do app já roda sobre o canvas levemente azulado.
 */
@Composable
internal fun AuthScaffold(
    title: String,
    subtitle: String,
    backContentDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CedarTopBar(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
                backContentDescription = backContentDescription,
            )
        },
        containerColor = CedarTokens.colors.canvas,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            content = content,
        )
    }
}

/**
 * Mensagem de resultado do formulário — o erro do login, o "e-mail enviado" da
 * recuperação.
 *
 * `liveRegion` é o que faltava: a mensagem aparecia calada para um leitor de tela,
 * então quem não enxerga tocava em "Entrar", nada acontecia na leitura e não havia
 * como saber por quê.
 */
@Composable
internal fun AuthFormMessage(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
