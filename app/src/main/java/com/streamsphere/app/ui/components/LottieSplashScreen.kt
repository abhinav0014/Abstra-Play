package com.streamsphere.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.*

@Composable
fun LottieSplashScreen(onAnimationsComplete: () -> Unit) {

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_frame2.json")
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true
    )

    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!triggered && composition != null && progress >= 0.99f) {
            triggered = true
            onAnimationsComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
