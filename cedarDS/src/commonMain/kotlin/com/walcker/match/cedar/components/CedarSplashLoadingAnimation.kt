package com.walcker.match.cedar.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.walcker.match.cedar.generated.resources.Res
import com.walcker.match.cedar.tokens.CedarTokens
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieAnimatable
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
public fun CedarSplashLoadingAnimation(
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            Res.readBytes("files/splash_animation.lottie"),
        )
    }
    val animatable = rememberLottieAnimatable()

    LaunchedEffect(composition) {
        val loaded = composition ?: return@LaunchedEffect
        animatable.animate(composition = loaded, iterations = Compottie.IterateForever)
    }

    Box(
        modifier = modifier.background(CedarTokens.colors.splashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter =
                rememberLottiePainter(
                    composition = composition,
                    progress = { animatable.progress },
                ),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
