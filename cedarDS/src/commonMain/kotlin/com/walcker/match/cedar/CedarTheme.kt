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

@Composable
public fun CedarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brand: CedarBrand = CedarBrand.Blue,
    fontFamily: FontFamily = cedarFontFamily(),
    content: @Composable () -> Unit,
) {
    ProvideCedarTokens(darkTheme = darkTheme) {
        MaterialTheme(
            colorScheme =
                if (darkTheme) {
                    cedarDarkColorScheme(brand)
                } else {
                    cedarLightColorScheme(brand)
                },
            typography = cedarTypography(fontFamily),
            content = content,
        )
    }
}

@Composable
public fun cedarFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
