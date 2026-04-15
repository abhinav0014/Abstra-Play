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
    
    val frame2Done = frame2Composition != null && frame2Progress >= 0.99f
    
    LaunchedEffect(frame2Progress) {
    if (frame2Done) {
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
    }
}
