package com.streamsphere.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context  = LocalContext.current
    var darkMode by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(false) }
    var compactView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding      = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App branding
            item {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier  = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("📺", style = MaterialTheme.typography.displaySmall)
                        Column {
                            Text("StreamSphere", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Version 1.0 · Global TV Channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Appearance
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsToggleItem(
                    icon    = Icons.Outlined.DarkMode,
                    title   = "Dark Theme",
                    subtitle = "Use dark background",
                    checked  = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }
            item {
                SettingsToggleItem(
                    icon    = Icons.Outlined.ViewCompact,
                    title   = "Compact View",
                    subtitle = "Show more channels per row",
                    checked  = compactView,
                    onCheckedChange = { compactView = it }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Notifications
            item { SettingsSectionHeader("Notifications") }
            item {
                SettingsToggleItem(
                    icon    = Icons.Outlined.Notifications,
                    title   = "Channel Alerts",
                    subtitle = "Notify when favourite channels go live",
                    checked  = notifications,
                    onCheckedChange = { notifications = it }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Widget
            item { SettingsSectionHeader("Widget") }
            item {
                SettingsClickItem(
                    icon     = Icons.Outlined.Widgets,
                    title    = "Widget Setup",
                    subtitle = "Add StreamSphere widget to your home screen"
                ) { /* Handled by Android */ }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // About
            item { SettingsSectionHeader("About") }
            item {
                SettingsClickItem(
                    icon     = Icons.Outlined.Source,
                    title    = "Data Source",
                    subtitle = "iptv-org · Open-source IPTV data"
                ) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/iptv-org/iptv"))
                    )
                }
            }
            item {
                SettingsClickItem(
                    icon     = Icons.Outlined.Info,
                    title    = "Open Source Licences",
                    subtitle = "View third-party licences"
                ) { }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsClickItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(14.dp),
        colors  = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
