package com.streamsphere.app.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.streamsphere.app.data.model.ChannelUiModel
import com.streamsphere.app.ui.components.*
import com.streamsphere.app.ui.theme.*
import com.streamsphere.app.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    channelId: String,
    autoPlay: Boolean = false,
    onBack: () -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val channels by viewModel.filteredChannels.collectAsState()
    val channel  = remember(channels, channelId) { channels.find { it.id == channelId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(channel?.name ?: "Channel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    channel?.let { ch ->
                        IconButton(onClick = { viewModel.toggleFavourite(ch) }) {
                            Icon(
                                imageVector        = if (ch.isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint               = if (ch.isFavourite) MaterialTheme.colorScheme.error
                                                     else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (channel == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            DetailContent(
                channel  = channel,
                autoPlay = autoPlay,
                onWidget = { viewModel.toggleWidget(channel) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun DetailContent(
    channel: ChannelUiModel,
    autoPlay: Boolean,
    onWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val categoryColor = when {
        channel.categories.any { it in listOf("music", "entertainment") } -> MusicPurple
        channel.categories.any { it in listOf("science", "education", "kids") } -> ScienceBlue
        channel.country.contains("Nepal", ignoreCase = true) -> NepalRed
        channel.country.contains("India", ignoreCase = true) -> IndiaOrange
        else -> Primary
    }

    // ── ExoPlayer setup ──────────────────────────────────────────────────────
    val hasStream = channel.streamUrl != null
    var isPlaying by remember { mutableStateOf(autoPlay && hasStream) }
    var playerError by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember(channel.streamUrl) {
        if (channel.streamUrl == null) return@remember null
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(channel.streamUrl)))
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = autoPlay
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playerError = "Stream unavailable: ${error.message}"
                    isPlaying = false
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    // Lifecycle management for player
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            exoPlayer?.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Video Player / Thumbnail Card ────────────────────────────────────
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasStream && exoPlayer != null) {
                    if (isPlaying || autoPlay) {
                        // Actual player view
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Thumbnail / play button overlay
                        if (channel.logoUrl != null) {
                            AsyncImage(
                                model              = channel.logoUrl,
                                contentDescription = channel.name,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
                            )
                        } else {
                            Text(channel.countryFlag, style = MaterialTheme.typography.displayLarge)
                        }
                        // Play button overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = {
                                    playerError = null
                                    exoPlayer.playWhenReady = true
                                    isPlaying = true
                                },
                                modifier = Modifier.size(64.dp),
                                colors   = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = categoryColor
                                )
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    modifier           = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Error overlay
                    playerError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint     = Color(0xFFFC8181),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text  = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // No stream available
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(categoryColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (channel.logoUrl != null) {
                                AsyncImage(
                                    model              = channel.logoUrl,
                                    contentDescription = channel.name,
                                    contentScale       = ContentScale.Fit,
                                    modifier           = Modifier.size(120.dp)
                                )
                            } else {
                                Text(channel.countryFlag, style = MaterialTheme.typography.displayLarge)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text  = "No stream available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── Channel info ──────────────────────────────────────────────────────
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
            if (channel.streamUrl != null) {
                LiveBadge()
            }
        }

        // Categories
        if (channel.categories.isNotEmpty()) {
            Text(
                "Categories",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                channel.categories.forEach { cat -> CategoryChip(cat, categoryColor) }
            }
        }

        // ── Widget button ─────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onWidget,
            modifier = Modifier.fillMaxWidth(),
            border   = BorderStroke(
                1.dp,
                if (channel.isWidget) categoryColor else MaterialTheme.colorScheme.outline.copy(0.5f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (channel.isWidget) categoryColor
                               else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector        = if (channel.isWidget) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (channel.isWidget) "Remove from Widget" else "Add to Home Screen Widget")
        }

        Spacer(Modifier.height(32.dp))
    }
}
