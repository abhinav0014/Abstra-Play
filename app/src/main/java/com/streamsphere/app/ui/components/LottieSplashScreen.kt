package com.streamsphere.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.*

@Composable
fun LottieSplashScreen(onAnimationsComplete: () -> Unit) {

    // --- Frame 2 (Background) ---
    val frame2Composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_frame2.json")
    )
    val frame2Progress by animateLottieCompositionAsState(
        composition = frame2Composition,
        iterations = 1,
        isPlaying = true
    )

    // --- Frame 1 (Foreground/Circular) ---
    val frame1Composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("splash_frame1.json")
    )
    val frame1Progress by animateLottieCompositionAsState(
        composition = frame1Composition,
        iterations = 1,
        isPlaying = true
    )

    // Completion Logic
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
            .background(Color.Transparent) // Changed to transparent as requested
    ) {
        // FRAME 2 — Background Layer
        LottieAnimation(
            composition = frame2Composition,
            progress = { frame2Progress },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f), // Explicit lower depth
            contentScale = ContentScale.Crop
        )

        // FRAME 1 — Circular Foreground Layer
        LottieAnimation(
            composition = frame1Composition,
            progress = { frame1Progress },
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .zIndex(2f) // Ensures it stays on top
                .clip(CircleShape) // Makes the frame appear circular
                .background(Color.Transparent), // Ensures the frame container doesn't block background
            contentScale = ContentScale.Fit
        )
    }
}
