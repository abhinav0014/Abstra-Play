package com.streamsphere.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.streamsphere.app.data.cast.ChromecastState
import com.streamsphere.app.data.dlna.DlnaDevice

/**
 * Bottom sheet that shows two sections:
 *  1. Chromecast — shows current session status with a stop option, or a "Cast via…" button
 *     that opens the system MediaRoute dialog. When a session is already active the
 *     [onChromeCastSelected] callback triggers an immediate media load on the receiver.
 *  2. DLNA / UPnP — lists discovered MediaRenderer devices; selecting one sends the stream.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDevicePickerSheet(
    // DLNA
    dlnaRenderers: List<DlnaDevice>,
    dlnaIsBound: Boolean,
    onDlnaDeviceSelected: (DlnaDevice) -> Unit,
    onDlnaRefresh: () -> Unit,
    // Chromecast
    chromecastState: ChromecastState,
    onChromeCastSelected: () -> Unit,
    onStopChromecast: () -> Unit,
    // Sheet
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Cast to device", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDlnaRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan for devices")
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // ── Chromecast section ─────────────────────────────────────────
            Text(
                "Chromecast",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (chromecastState.isCasting) {
                // Currently casting — show device name + stop option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStopChromecast(); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CastConnected,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Casting to ${chromecastState.deviceName ?: "Chromecast"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Tap to stop casting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.CastConnected,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Also allow re-sending the current stream to the existing session
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChromeCastSelected(); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(40.dp))
                    Text(
                        "Send current stream to Chromecast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // No active session — prompt the user to connect via system dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChromeCastSelected(); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cast,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Cast via Chromecast / Google Cast…",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Use the Cast button (⋮ menu) to connect first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Show any Chromecast error inline
            chromecastState.errorMessage?.let { err ->
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        err,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── DLNA / UPnP section ────────────────────────────────────────
            Text(
                "DLNA / UPnP",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            when {
                !dlnaIsBound -> {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Connecting to UPnP service…")
                    }
                }

                dlnaRenderers.isEmpty() -> {
                    Text(
                        text = "No DLNA renderers found on your network.\n" +
                               "Make sure your TV or device is on the same Wi-Fi.",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    // Prompt to rescan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick  = onDlnaRefresh,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan again")
                        }
                    }
                }

                else -> {
                    LazyColumn {
                        items(dlnaRenderers, key = { it.udn }) { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDlnaDeviceSelected(device); onDismiss() }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(device.friendlyName, style = MaterialTheme.typography.bodyLarge)
                                    device.manufacturer?.let { mfr ->
                                        Text(
                                            buildString {
                                                append(mfr)
                                                device.modelName?.let { append(" · $it") }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } ?: device.modelName?.let { model ->
                                        Text(model, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }
}