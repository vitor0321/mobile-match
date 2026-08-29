package com.walcker.match.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.walcker.match.app.generated.resources.Res
import com.walcker.match.cedar.tokens.CedarTokens
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieAnimatable
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay

@Composable
internal fun SplashScreen(
    modifier: Modifier = Modifier,
    onReady: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            Res.readBytes("files/splash_animation.lottie"),
        )
    }
    val animatable = rememberLottieAnimatable()

    var handedOff by remember { mutableStateOf(false) }

    LaunchedEffect(composition) {
        val loaded = composition ?: return@LaunchedEffect
        withFrameNanos { }
        if (!handedOff) {
            handedOff = true
            onReady?.invoke()
        }
        animatable.animate(composition = loaded, iterations = Compottie.IterateForever)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CedarTokens.colors.splashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                progress = { animatable.progress },
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
