package com.streamsphere.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        // Full-screen player — no scaffold, no top bar
        channel?.let { ch ->
            FullscreenPlayer(
                channel      = ch,
                autoPlay     = autoPlay,
                onExitFullscreen = { isFullscreen = false },
                onWidget     = { viewModel.toggleWidget(ch) }
            )
        }
    } else {
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
                                    tint               = if (ch.isFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
                    channel          = channel,
                    autoPlay         = autoPlay,
                    onWidget         = { viewModel.toggleWidget(channel) },
                    onEnterFullscreen = { isFullscreen = true },
                    modifier         = Modifier.padding(padding)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inline (portrait) player + channel info
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

    val categoryColor = categoryColorFor(channel)
    val hasStream = channel.streamUrl != null
    var isPlaying   by remember { mutableStateOf(autoPlay && hasStream) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val exoPlayer = rememberExoPlayer(context, channel.streamUrl, autoPlay) { err ->
        playerError = err
        isPlaying = false
    }
    LaunchedEffect(exoPlayer) {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); exoPlayer?.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Inline Player (16:9 aspect) ───────────────────────────────────────
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
                            useController = false          // we draw our own controls
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
                    // Thumbnail overlay before play starts
                    ThumbnailOverlay(channel, categoryColor) {
                        playerError = null
                        exoPlayer.playWhenReady = true
                    }
                }

                // Custom controls overlay
                InlinePlayerControls(
                    isPlaying        = isPlaying,
                    channel          = channel,
                    categoryColor    = categoryColor,
                    onPlayPause      = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    onFullscreen     = onEnterFullscreen
                )

                playerError?.let { err ->
                    ErrorOverlay(err) {
                        playerError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            } else {
                NoStreamOverlay(channel, categoryColor)
            }
        }

        // ── Channel metadata ──────────────────────────────────────────────────
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
                    channel.categories.forEach { cat -> CategoryChip(cat, categoryColor) }
                }
            }

            OutlinedButton(
                onClick  = onWidget,
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, if (channel.isWidget) categoryColor else MaterialTheme.colorScheme.outline.copy(0.5f)),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = if (channel.isWidget) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant)
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
// Full-screen player
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenPlayer(
    channel: ChannelUiModel,
    autoPlay: Boolean,
    onExitFullscreen: () -> Unit,
    onWidget: () -> Unit
) {
    val context   = LocalContext.current
    val activity  = context as Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope     = rememberCoroutineScope()

    val categoryColor = categoryColorFor(channel)
    var isPlaying     by remember { mutableStateOf(true) }
    var playerError   by remember { mutableStateOf<String?>(null) }
    var isLocked      by remember { mutableStateOf(false) }
    var showControls  by remember { mutableStateOf(true) }
    var isRotationLocked by remember { mutableStateOf(false) }

    // Volume & Brightness state
    val audioManager  = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume     = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volumeLevel   by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var brightnessLevel by remember {
        val cur = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) { 0.5f }
        mutableStateOf(cur)
    }

    // Enter true fullscreen
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

    // Auto-hide controls
    LaunchedEffect(showControls) {
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
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                Lifecycle.Event.ON_PAUSE  -> exoPlayer?.pause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); exoPlayer?.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (!isLocked) {
                            // Left half = brightness, right half = volume
                            val xPos = change.position.x
                            val screenWidth = size.width.toFloat()
                            val delta = -dragAmount / size.height.toFloat() * 0.5f
                            if (xPos < screenWidth / 2) {
                                brightnessLevel = (brightnessLevel + delta).coerceIn(0f, 1f)
                                try {
                                    val lp = activity.window.attributes
                                    lp.screenBrightness = brightnessLevel
                                    activity.window.attributes = lp
                                } catch (e: Exception) {}
                            } else {
                                volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (volumeLevel * maxVolume).roundToInt(),
                                    0
                                )
                            }
                        }
                    }
                )
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

        // Touch to toggle controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            showControls = true
                        }
                    }
                }
        )

        // Locked overlay — only show lock icon when locked
        if (isLocked) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedVisibility(visible = showControls) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.Black.copy(0.6f)
                    ) {
                        IconButton(
                            onClick = { isLocked = false; showControls = true },
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Filled.Lock, "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        } else {
            // Full controls overlay
            AnimatedVisibility(
                visible = showControls,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                FullscreenControls(
                    channel          = channel,
                    isPlaying        = isPlaying,
                    categoryColor    = categoryColor,
                    volumeLevel      = volumeLevel,
                    brightnessLevel  = brightnessLevel,
                    isRotationLocked = isRotationLocked,
                    onPlayPause      = { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() },
                    onExitFullscreen = onExitFullscreen,
                    onLock           = { isLocked = true },
                    onToggleRotation = {
                        isRotationLocked = !isRotationLocked
                        activity.requestedOrientation = if (isRotationLocked)
                            ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        else
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    },
                    onVolumeChange = { v ->
                        volumeLevel = v
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (v * maxVolume).roundToInt(), 0)
                    },
                    onBrightnessChange = { b ->
                        brightnessLevel = b
                        try {
                            val lp = activity.window.attributes
                            lp.screenBrightness = b
                            activity.window.attributes = lp
                        } catch (e: Exception) {}
                    }
                )
            }

            playerError?.let { err ->
                ErrorOverlay(err) {
                    playerError = null
                    exoPlayer?.prepare()
                    exoPlayer?.play()
                }
            }

            // Volume/Brightness gesture hint indicators (left & right edges)
            BrightnessVolumeIndicators(brightnessLevel, volumeLevel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fullscreen controls UI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullscreenControls(
    channel: ChannelUiModel,
    isPlaying: Boolean,
    categoryColor: Color,
    volumeLevel: Float,
    brightnessLevel: Float,
    isRotationLocked: Boolean,
    onPlayPause: () -> Unit,
    onExitFullscreen: () -> Unit,
    onLock: () -> Unit,
    onToggleRotation: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        // ── Top bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExitFullscreen) {
                Icon(Icons.Filled.FullscreenExit, "Exit fullscreen", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(channel.name, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            if (channel.streamUrl != null) LiveBadge()
            Spacer(Modifier.width(8.dp))

            // Rotation lock
            IconButton(onClick = onToggleRotation) {
                Icon(
                    imageVector = if (isRotationLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                    contentDescription = "Rotation lock",
                    tint = if (isRotationLocked) categoryColor else Color.White
                )
            }
            // Screen lock
            IconButton(onClick = onLock) {
                Icon(Icons.Outlined.LockOpen, "Lock screen", tint = Color.White)
            }
        }

        // ── Center play/pause ─────────────────────────────────────────────
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Seek back 10s
            IconButton(
                onClick  = {},  // ExoPlayer seek needs player ref — wired via callback in real use
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Replay10, "Replay 10s", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // Play/Pause
            FilledIconButton(
                onClick  = onPlayPause,
                modifier = Modifier.size(64.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(containerColor = categoryColor)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Seek forward 10s
            IconButton(
                onClick  = {},
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // ── Bottom bar: Volume & Brightness sliders ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Volume
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (volumeLevel == 0f) Icons.Filled.VolumeOff
                                  else if (volumeLevel < 0.5f) Icons.Filled.VolumeDown
                                  else Icons.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value         = volumeLevel,
                    onValueChange = onVolumeChange,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor       = categoryColor,
                        activeTrackColor = categoryColor,
                        inactiveTrackColor = Color.White.copy(0.3f)
                    )
                )
                Text("${(volumeLevel * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(36.dp))
            }

            // Brightness
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (brightnessLevel < 0.3f) Icons.Filled.BrightnessLow
                                  else if (brightnessLevel < 0.7f) Icons.Filled.BrightnessMedium
                                  else Icons.Filled.BrightnessHigh,
                    contentDescription = "Brightness",
                    tint     = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value         = brightnessLevel,
                    onValueChange = onBrightnessChange,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor       = Color(0xFFFBD38D),
                        activeTrackColor = Color(0xFFFBD38D),
                        inactiveTrackColor = Color.White.copy(0.3f)
                    )
                )
                Text("${(brightnessLevel * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(36.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inline controls overlay (portrait player)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InlinePlayerControls(
    isPlaying: Boolean,
    channel: ChannelUiModel,
    categoryColor: Color,
    onPlayPause: () -> Unit,
    onFullscreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isPlaying) 0f else 0.2f))
    ) {
        // Fullscreen button top-right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onFullscreen) {
                Icon(Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White)
            }
        }

        // Center play/pause
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick  = onPlayPause,
                modifier = Modifier.size(52.dp),
                colors   = IconButtonDefaults.filledIconButtonColors(containerColor = categoryColor.copy(0.85f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gesture volume/brightness edge indicators
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrightnessVolumeIndicators(brightness: Float, volume: Float) {
    Box(Modifier.fillMaxSize()) {
        // Left edge — brightness
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterStart)
                .padding(vertical = 80.dp)
                .background(Color.White.copy(0.15f), RoundedCornerShape(2.dp))
        ) {
            Spacer(Modifier.weight(1f - brightness))
            Box(Modifier.weight(brightness.coerceAtLeast(0.01f)).fillMaxWidth().background(Color(0xFFFBD38D).copy(0.8f), RoundedCornerShape(2.dp)))
        }
        // Right edge — volume
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterEnd)
                .padding(vertical = 80.dp)
                .background(Color.White.copy(0.15f), RoundedCornerShape(2.dp))
        ) {
            Spacer(Modifier.weight(1f - volume))
            Box(Modifier.weight(volume.coerceAtLeast(0.01f)).fillMaxWidth().background(Color(0xFF4F8EF7).copy(0.8f), RoundedCornerShape(2.dp)))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThumbnailOverlay(channel: ChannelUiModel, categoryColor: Color, onPlay: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(categoryColor.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logoUrl != null) {
            AsyncImage(model = channel.logoUrl, contentDescription = channel.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(32.dp))
        } else {
            Text(channel.countryFlag, style = MaterialTheme.typography.displayLarge)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        FilledIconButton(
            onClick  = onPlay,
            modifier = Modifier.size(56.dp),
            colors   = IconButtonDefaults.filledIconButtonColors(containerColor = categoryColor)
        ) {
            Icon(Icons.Filled.PlayArrow, "Play", modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.75f)),
        contentAlignment = Alignment.Center
    ) {
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
private fun NoStreamOverlay(channel: ChannelUiModel, categoryColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(categoryColor.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
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
): ExoPlayer? {
    return remember(streamUrl) {
        if (streamUrl == null) return@remember null
        ExoPlayer.Builder(context).build().apply {
            val mediaSource = HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = autoPlay
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { onError("Stream unavailable") }
            })
        }
    }
}

private fun categoryColorFor(channel: ChannelUiModel): Color = when {
    channel.categories.any { it in listOf("music", "entertainment") } -> MusicPurple
    channel.categories.any { it in listOf("science", "education", "kids") } -> ScienceBlue
    channel.country.contains("Nepal", ignoreCase = true) -> NepalRed
    channel.country.contains("India", ignoreCase = true) -> IndiaOrange
    else -> Primary
}
