package com.walcker.match.cedar

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.walcker.match.cedar.generated.resources.Res
import com.walcker.match.cedar.generated.resources.inter_bold
import com.walcker.match.cedar.generated.resources.inter_medium
import com.walcker.match.cedar.generated.resources.inter_regular
import com.walcker.match.cedar.generated.resources.inter_semibold
import com.walcker.match.cedar.tokens.CedarBrand
import com.walcker.match.cedar.tokens.ProvideCedarTokens
import com.walcker.match.cedar.tokens.cedarDarkColorScheme
import com.walcker.match.cedar.tokens.cedarLightColorScheme
import com.walcker.match.cedar.tokens.cedarTypography
import org.jetbrains.compose.resources.Font

/**
 * O tema do app: esquema de cores do Material, escala tipográfica e os tokens Cedar.
 *
 * Este arquivo era o resto do tema antigo, herdado do produto anterior. Três coisas
 * que estavam nele e não voltam:
 *
 * - `CedarTypography` com um único estilo `venueName`, e o objeto `CedarTheme` que
 *   servia esse estilo. **Nenhuma chamada no app inteiro** — o `MatchCard`, que seria
 *   o cliente natural, usa `MaterialTheme.typography`. A escala agora é uma
 *   [androidx.compose.material3.Typography] completa, em `cedarTypography()`.
 * - `onSurfaceVariant = #B6B6B6`, que dá **2,03:1** com branco. Era a cor de quase todo
 *   subtítulo e rótulo do app — todos reprovavam no WCAG AA. Virou `Ink500`, 4,9:1.
 * - `error = #FDA291` (salmão), **2:1** com branco. A mensagem de erro é o texto que
 *   mais precisa ser lido e era o menos legível da tela. E `ErrorBgDark` era
 *   `Color(0xFFF3A1D1B)` — nove dígitos hexadecimais, quando `Color(Long)` espera oito
 *   (`0xAARRGGBB`). Aquele valor estourava 32 bits e nunca produziu a cor pretendida.
 *
 * @param brand qual cor carrega a marca. O padrão é [CedarBrand.Blue], como no Figma:
 *   azul é o que se aperta, verde é o que diz que ainda tem vaga. `CedarBrand.Green`
 *   devolve a identidade anterior num esquema simétrico — mas com o verde escurecido
 *   para `#087A41`, porque o `#06C167` de antes tem 2,4:1 com branco e "Entrar na
 *   partida" em branco sobre ele já reprovava.
 * @param fontFamily a Inter, quando estiver empacotada em `composeResources/font/`.
 *   Até lá cada plataforma usa a sua padrão, que é por que Android e iOS ainda
 *   parecem dois apps.
 */
@Composable
public fun CedarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brand: CedarBrand = CedarBrand.Blue,
    fontFamily: FontFamily = cedarFontFamily(),
    content: @Composable () -> Unit,
) {
    ProvideCedarTokens(darkTheme = darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                cedarDarkColorScheme(brand)
            } else {
                cedarLightColorScheme(brand)
            },
            typography = cedarTypography(fontFamily),
            content = content,
        )
    }
}

/**
 * FontFamily com a Inter empacotada via composeResources.
 *
 * Carrega os 4 pesos (Regular, Medium, SemiBold, Bold) do
 * `commonMain/composeResources/font/Inter-*.ttf`.
 * Usada como padrão no [CedarTheme] para garantir identidade visual
 * idêntica entre Android e iOS.
 *
 * É `@Composable` porque o loader de fontes do compose-resources resolve o
 * recurso na composição; por isso não pode ser um `val` de topo.
 */
@Composable
public fun cedarFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)
