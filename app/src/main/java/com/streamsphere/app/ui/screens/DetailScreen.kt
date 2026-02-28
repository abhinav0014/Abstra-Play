package com.streamsphere.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamsphere.app.data.model.ChannelUiModel
import com.streamsphere.app.ui.components.*
import com.streamsphere.app.ui.theme.*
import com.streamsphere.app.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val channels by viewModel.filteredChannels.collectAsState()
    val channel  = remember(channels, channelId) { channels.find { it.id == channelId } }
    val context  = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title       = { Text(channel?.name ?: "Channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    channel?.let { ch ->
                        IconButton(onClick = { viewModel.toggleFavourite(ch) }) {
                            Icon(
                                imageVector = if (ch.isFavourite) Icons.Filled.Favorite
                                              else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (ch.isFavourite) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (channel == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            DetailContent(
                channel    = channel,
                onWidget   = { viewModel.toggleWidget(channel) },
                onOpenUrl  = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier   = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DetailContent(
    channel: ChannelUiModel,
    onWidget: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when {
        channel.categories.any { it in listOf("music","entertainment") } -> MusicPurple
        channel.categories.any { it in listOf("science","education","kids") } -> ScienceBlue
        channel.country.contains("Nepal", ignoreCase = true) -> NepalRed
        channel.country.contains("India", ignoreCase = true) -> IndiaOrange
        else -> Primary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero card
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(categoryColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl != null) {
                    AsyncImage(
                        model              = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    )
                } else {
                    Text(
                        text  = channel.countryFlag,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }
        }

        // Info
        Text(channel.name, style = MaterialTheme.typography.headlineMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(channel.countryFlag, style = MaterialTheme.typography.titleLarge)
            Text(
                text  = channel.country,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Category chips
        if (channel.categories.isNotEmpty()) {
            Text("Categories", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                channel.categories.forEach { cat ->
                    CategoryChip(cat, categoryColor)
                }
            }
        }

        // Stream card
        if (channel.streamUrl != null) {
            OutlinedCard(
                onClick = { onOpenUrl(channel.streamUrl) },
                shape   = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint     = categoryColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Watch Live", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text  = "Open stream in browser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.OpenInNew, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Widget button
        Button(
            onClick = onWidget,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (channel.isWidget) categoryColor else MaterialTheme.colorScheme.surfaceVariant,
                contentColor   = if (channel.isWidget) MaterialTheme.colorScheme.onPrimary
                                 else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = if (channel.isWidget) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (channel.isWidget) "Remove from Widget" else "Add to Widget")
        }

        Spacer(Modifier.height(32.dp))
    }
}
