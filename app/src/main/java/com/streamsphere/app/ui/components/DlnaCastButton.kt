package com.streamsphere.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.streamsphere.app.viewmodel.DlnaCastState

/**
 * Cast button shown inside the player overlay (both portrait and fullscreen).
 *
 * Displays a filled "cast connected" icon in the theme primary colour whenever
 * either a DLNA or Chromecast session is active.  Falls back to a plain white
 * cast icon otherwise.
 */
@Composable
fun DlnaCastButton(
    castState: DlnaCastState,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector        = if (castState.isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
            contentDescription = if (castState.isCasting) "Stop casting" else "Cast to device",
            tint = if (castState.isCasting) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}