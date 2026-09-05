package com.walcker.match.cedar.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.generated.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
public fun CedarHistoryActiveAnimation(
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            Res.readBytes("files/history_active.lottie"),
        )
    }
    Image(
        painter =
            rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
