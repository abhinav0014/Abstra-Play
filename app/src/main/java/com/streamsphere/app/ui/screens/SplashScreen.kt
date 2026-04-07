package com.streamsphere.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

@Composable
fun SplashScreen(onAnimationsComplete: () -> Unit) {

    // --- Frame 2 (background) state ---
    val frame2Composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_frame2.json")
    )
    val frame2Progress by animateLottieCompositionAsState(
        composition = frame2Composition,
        iterations = 1,
        isPlaying = true
    )

    // --- Frame 1 (foreground) state ---
    val frame1Composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_frame1.json")
    )
    val frame1Progress by animateLottieCompositionAsState(
        composition = frame1Composition,
        iterations = 1,
        isPlaying = true
    )

    // Navigate when BOTH reach the end (progress == 1f)
    val frame1Done = frame1Progress == 1f && frame1Composition != null
    val frame2Done = frame2Progress == 1f && frame2Composition != null

    LaunchedEffect(frame1Done, frame2Done) {
        if (frame1Done && frame2Done) {
            onAnimationsComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // FRAME 2 — full screen, behind everything
        LottieAnimation(
            composition = frame2Composition,
            progress = { frame2Progress },
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // FRAME 1 — fixed size, centered, on top
        LottieAnimation(
            composition = frame1Composition,
            progress = { frame1Progress },
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
        )
    }
}
