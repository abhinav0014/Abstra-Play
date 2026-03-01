package com.streamsphere.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    channelId: String,
    autoPlay: Boolean = false,
    startInFullscreen: Boolean = false,
    onBack: () -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val channels by viewModel.filteredChannels.collectAsState()
    val channel  = remember(channels, channelId) { channels.find { it.id == channelId } }

    // Start fullscreen immediately if launched from widget
    var isFullscreen by remember { mutableStateOf(startInFullscreen) }

    if (isFullscreen) {
        channel?.let { ch ->
            FullscreenPlayer(
                channel          = ch,
                onExitFullscreen = { isFullscreen = false },
                onFavourite      = { viewModel.toggleFavourite(ch) },
                onWidget         = { viewModel.toggleWidget(ch) }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(channel?.name ?: "Channel") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Back")
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
                    channel           = channel,
                    autoPlay          = autoPlay,
                    onWidget          = { viewModel.toggleWidget(channel) },
                    onEnterFullscreen = { isFullscreen = true },
                    modifier          = Modifier.padding(padding)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Portrait inline player + channel info
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun DetailContent(
    channel: ChannelUiModel,
    autoPlay: Boolean,
    onWidget: () -> Unit,
    onEnterFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val catColor  = categoryColorFor(channel)
    val hasStream = channel.streamUrl != null

    var isPlaying   by remember { mutableStateOf(autoPlay && hasStream) }
    var playerError by remember { mutableStateOf<String?>(null) }

    val exoPlayer = rememberExoPlayer(context, channel.streamUrl, autoPlay) { err ->
        playerError = err; isPlaying = false
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })
    }

    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); exoPlayer?.release() }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 16:9 player area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasStream && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player        = exoPlayer
                            useController = false
                            resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams  = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isPlaying && playerError == null) {
                    ThumbnailOverlay(channel, catColor) {
                        playerError = null
                        exoPlayer.playWhenReady = true
                    }
                }

                // Inline overlay controls
                Box(Modifier.fillMaxSize()) {
                    // Fullscreen button - top right
                    IconButton(
                        onClick  = onEnterFullscreen,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White,
                            modifier = Modifier.size(28.dp))
                    }
                    // Play/Pause center
                    FilledIconButton(
                        onClick  = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        modifier = Modifier.align(Alignment.Center).size(52.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(containerColor = catColor.copy(0.85f))
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, modifier = Modifier.size(32.dp)
                        )
                    }
                }

                playerError?.let { err -> ErrorOverlay(err) { playerError = null; exoPlayer.prepare(); exoPlayer.play() } }
            } else {
                NoStreamOverlay(channel, catColor)
            }
        }

        // Channel info
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(channel.name, style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(channel.countryFlag, style = MaterialTheme.typography.titleLarge)
                Text(channel.country, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (channel.streamUrl != null) LiveBadge()
            }
            if (channel.categories.isNotEmpty()) {
                Text("Categories", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    channel.categories.forEach { cat -> CategoryChip(cat, catColor) }
                }
            }
            OutlinedButton(
                onClick  = onWidget,
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, if (channel.isWidget) catColor else MaterialTheme.colorScheme.outline.copy(0.5f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (channel.isWidget) catColor else MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(if (channel.isWidget) Icons.Filled.Widgets else Icons.Outlined.Widgets, null)
                Spacer(Modifier.width(8.dp))
                Text(if (channel.isWidget) "Remove from Widget" else "Add to Home Screen Widget")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRUE fullscreen landscape player
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenPlayer(
    channel: ChannelUiModel,
    onExitFullscreen: () -> Unit,
    onFavourite: () -> Unit,
    onWidget: () -> Unit
) {
    val context   = LocalContext.current
    val activity  = context as Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val catColor  = categoryColorFor(channel)

    // Audio manager — safe, no permissions needed
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol       = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var isPlaying        by remember { mutableStateOf(true) }
    var playerError      by remember { mutableStateOf<String?>(null) }
    var isLocked         by remember { mutableStateOf(false) }
    var showControls     by remember { mutableStateOf(true) }
    var isRotLocked      by remember { mutableStateOf(false) }

    // Volume — clamped 0..1, uses stream volume (no permission needed)
    var volumeLevel      by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }

    // Brightness — uses ONLY WindowManager.LayoutParams (no WRITE_SETTINGS permission needed, no crash)
    var brightnessLevel  by remember {
        val cur = activity.window.attributes.screenBrightness
        mutableStateOf(if (cur < 0) 0.5f else cur)   // -1 means "system default" → use 0.5 as display value
    }

    // Helper: apply brightness safely via WindowManager only
    fun applyBrightness(value: Float) {
        val clamped = value.coerceIn(0.01f, 1.0f)  // never go to 0 (screen fully off = crash risk)
        val lp = activity.window.attributes
        lp.screenBrightness = clamped
        activity.window.attributes = lp
    }

    // Helper: apply volume safely
    fun applyVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (clamped * maxVol).roundToInt().coerceIn(0, maxVol.roundToInt()),
            0
        )
    }

    // Force landscape + hide system bars
    LaunchedEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.apply {
                hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    // Restore on exit
    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Restore system brightness to "follow system"
            val lp = activity.window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = lp
            // Restore bars
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.show(
                    android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
                )
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    // Auto-hide controls after 4s
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    val exoPlayer = rememberExoPlayer(context, channel.streamUrl, true) { err ->
        playerError = err; isPlaying = false
    }
    LaunchedEffect(exoPlayer) {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })
    }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); exoPlayer?.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Vertical drag gesture: left half = brightness, right half = volume
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val delta     = -dragAmount / size.height.toFloat()
                        val isLeft    = change.position.x < size.width / 2f
                        if (isLeft) {
                            val newBright = (brightnessLevel + delta).coerceIn(0.01f, 1.0f)
                            brightnessLevel = newBright
                            applyBrightness(newBright)
                        } else {
                            val newVol = (volumeLevel + delta).coerceIn(0f, 1f)
                            volumeLevel = newVol
                            applyVolume(newVol)
                        }
                        showControls = true
                    }
                }
            }
            // Tap anywhere to toggle controls
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        showControls = !showControls
                    }
                }
            }
    ) {
        // Video surface
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player        = exoPlayer
                        useController = false
                        resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams  = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Always-visible edge indicators for volume (right) and brightness (left)
        EdgeLevelBars(brightnessLevel = brightnessLevel, volumeLevel = volumeLevel)

        if (isLocked) {
            // Locked: only show unlock icon on tap
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(0.55f)) {
                        IconButton(
                            onClick  = { isLocked = false; showControls = true },
                            modifier = Modifier.padding(12.dp).size(48.dp)
                        ) {
                            Icon(Icons.Filled.Lock, "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        } else {
            // Full controls overlay
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                FullscreenControls(
                    channel          = channel,
                    isPlaying        = isPlaying,
                    catColor         = catColor,
                    isRotLocked      = isRotLocked,
                    onPlayPause      = { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() },
                    onReplay         = { exoPlayer?.seekBack() },
                    onForward        = { exoPlayer?.seekForward() },
                    onExitFullscreen = onExitFullscreen,
                    onLock           = { isLocked = true; showControls = false },
                    onToggleRot      = {
                        isRotLocked = !isRotLocked
                        activity.requestedOrientation =
                            if (isRotLocked) ActivityInfo.SCREEN_ORIENTATION_LOCKED
                            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                )
            }

            playerError?.let { err ->
                ErrorOverlay(err) { playerError = null; exoPlayer?.prepare(); exoPlayer?.play() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fullscreen controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullscreenControls(
    channel: ChannelUiModel,
    isPlaying: Boolean,
    catColor: Color,
    isRotLocked: Boolean,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onForward: () -> Unit,
    onExitFullscreen: () -> Unit,
    onLock: () -> Unit,
    onToggleRot: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitFullscreen) {
                Icon(Icons.Filled.FullscreenExit, "Exit fullscreen", tint = Color.White)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text  = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (channel.streamUrl != null) LiveBadge()
            Spacer(Modifier.width(8.dp))
            // Rotation lock
            IconButton(onClick = onToggleRot) {
                Icon(
                    imageVector        = if (isRotLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                    contentDescription = "Rotation",
                    tint               = if (isRotLocked) catColor else Color.White
                )
            }
            // Screen lock
            IconButton(onClick = onLock) {
                Icon(Icons.Outlined.LockOpen, "Lock", tint = Color.White)
            }
        }

        // Center play controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onReplay, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Replay10, "Replay 10s", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            FilledIconButton(
                onClick  = onPlayPause,
                modifier = Modifier.size(64.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(containerColor = catColor)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onForward, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Bottom hint text
        Text(
            text     = "← Swipe left edge: brightness  |  Swipe right edge: volume →",
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White.copy(0.55f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edge level bars (brightness left, volume right) — always visible
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EdgeLevelBars(brightnessLevel: Float, volumeLevel: Float) {
    Box(Modifier.fillMaxSize()) {
        // Left — brightness (amber)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .align(Alignment.CenterStart)
                .padding(vertical = 60.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(0.12f), RoundedCornerShape(3.dp))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(brightnessLevel.coerceIn(0.01f, 1f))
                    .align(Alignment.BottomStart)
                    .background(Color(0xFFFBD38D).copy(0.85f), RoundedCornerShape(3.dp))
            )
        }
        // Right — volume (blue)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .align(Alignment.CenterEnd)
                .padding(vertical = 60.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(0.12f), RoundedCornerShape(3.dp))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(volumeLevel.coerceIn(0.01f, 1f))
                    .align(Alignment.BottomStart)
                    .background(Color(0xFF4F8EF7).copy(0.85f), RoundedCornerShape(3.dp))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared small composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThumbnailOverlay(channel: ChannelUiModel, catColor: Color, onPlay: () -> Unit) {
    Box(Modifier.fillMaxSize().background(catColor.copy(0.08f)), contentAlignment = Alignment.Center) {
        if (channel.logoUrl != null) {
            AsyncImage(model = channel.logoUrl, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(32.dp))
        } else {
            Text(channel.countryFlag, style = MaterialTheme.typography.displayLarge)
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.30f)), contentAlignment = Alignment.Center) {
        FilledIconButton(onClick = onPlay, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = catColor)) {
            Icon(Icons.Filled.PlayArrow, "Play", modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.75f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFFC8181), modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry", color = Color.White) }
        }
    }
}

@Composable
private fun NoStreamOverlay(channel: ChannelUiModel, catColor: Color) {
    Box(Modifier.fillMaxSize().background(catColor.copy(0.08f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (channel.logoUrl != null) {
                AsyncImage(model = channel.logoUrl, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.size(100.dp))
            } else {
                Text(channel.countryFlag, style = MaterialTheme.typography.displayLarge)
            }
            Spacer(Modifier.height(8.dp))
            Text("No stream available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun rememberExoPlayer(
    context: Context,
    streamUrl: String?,
    autoPlay: Boolean,
    onError: (String) -> Unit
): ExoPlayer? = remember(streamUrl) {
    if (streamUrl == null) return@remember null
    ExoPlayer.Builder(context).build().apply {
        val src = HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
        setMediaSource(src)
        prepare()
        playWhenReady = autoPlay
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { onError("Stream unavailable") }
        })
    }
}

private fun categoryColorFor(channel: ChannelUiModel): Color = when {
    channel.categories.any { it in listOf("music","entertainment") } -> MusicPurple
    channel.categories.any { it in listOf("science","education","kids") } -> ScienceBlue
    channel.country.contains("Nepal", ignoreCase = true) -> NepalRed
    channel.country.contains("India", ignoreCase = true) -> IndiaOrange
    else -> Primary
}
