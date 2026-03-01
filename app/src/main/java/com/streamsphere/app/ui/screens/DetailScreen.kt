package com.streamsphere.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.streamsphere.app.data.model.StreamOption
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

    var isFullscreen by remember { mutableStateOf(startInFullscreen) }

    if (isFullscreen) {
        channel?.let { ch ->
            FullscreenPlayer(
                channel          = ch,
                onExitFullscreen = { isFullscreen = false },
                onFavourite      = { viewModel.toggleFavourite(ch) },
                onWidget         = { viewModel.toggleWidget(ch) },
                onSelectStream   = { idx -> viewModel.selectStream(ch.id, idx) }
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
                    onSelectStream    = { idx -> viewModel.selectStream(channel.id, idx) },
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
    onSelectStream: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val catColor  = categoryColorFor(channel)

    var isPlaying   by remember { mutableStateOf(autoPlay) }
    var playerError by remember { mutableStateOf<String?>(null) }

    // Rebuild player whenever the selected stream URL changes
    val currentUrl = channel.streamUrl
    val exoPlayer = rememberExoPlayer(context, currentUrl, autoPlay) { err ->
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

    // Feed picker sheet state
    var showFeedPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── 16:9 player area ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
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

                if (!isPlaying && playerError == null) {
                    ThumbnailOverlay(channel, catColor) {
                        playerError = null
                        exoPlayer.playWhenReady = true
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    IconButton(
                        onClick  = onEnterFullscreen,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Fullscreen, "Fullscreen", tint = Color.White,
                            modifier = Modifier.size(28.dp))
                    }
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

                playerError?.let { err ->
                    ErrorOverlay(err) { playerError = null; exoPlayer.prepare(); exoPlayer.play() }
                }
            } else {
                // No stream at all (shouldn't happen after filtering)
                Box(Modifier.fillMaxSize().background(catColor.copy(0.08f)), contentAlignment = Alignment.Center) {
                    Text("No stream available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Feed / Language selector bar ──────────────────────────────────────
        if (channel.hasMultipleFeeds) {
            FeedSelectorBar(
                channel        = channel,
                onOpenPicker   = { showFeedPicker = true },
                catColor       = catColor
            )
        }

        // ── Channel info ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(channel.name, style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(channel.countryFlag, style = MaterialTheme.typography.titleLarge)
                Text(channel.country, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LiveBadge()
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
            Spacer(Modifier.height(8.dp))
        }
    }

    // ── Feed picker bottom sheet ──────────────────────────────────────────────
    if (showFeedPicker) {
        FeedPickerSheet(
            channel        = channel,
            catColor       = catColor,
            onSelect       = { idx ->
                onSelectStream(idx)
                showFeedPicker = false
            },
            onDismiss      = { showFeedPicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feed selector bar (shown below the player when multiple feeds exist)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedSelectorBar(
    channel: ChannelUiModel,
    onOpenPicker: () -> Unit,
    catColor: Color
) {
    val current = channel.currentStream
    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Subtitles, null, tint = catColor, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = current?.feedName ?: "Default",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!current?.languageNames.isNullOrEmpty()) {
                    Text(
                        text  = current!!.languageNames.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            current?.quality?.let {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = catColor.copy(0.15f)
                ) {
                    Text(
                        text     = it,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = catColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            TextButton(onClick = onOpenPicker, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Change", style = MaterialTheme.typography.labelMedium, color = catColor)
                Icon(Icons.Filled.ExpandMore, null, modifier = Modifier.size(16.dp), tint = catColor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feed / Language picker bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPickerSheet(
    channel: ChannelUiModel,
    catColor: Color,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Language, null, tint = catColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Choose Feed / Language",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                "${channel.streamOptions.size} feeds available",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            channel.streamOptions.forEachIndexed { idx, option ->
                val isSelected = idx == channel.selectedStreamIndex
                FeedOptionRow(
                    option     = option,
                    isSelected = isSelected,
                    catColor   = catColor,
                    onClick    = { onSelect(idx) }
                )
                if (idx < channel.streamOptions.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
            }
        }
    }
}

@Composable
private fun FeedOptionRow(
    option: StreamOption,
    isSelected: Boolean,
    catColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color   = if (isSelected) catColor.copy(0.08f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.CheckCircle, null, tint = catColor, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Outlined.RadioButtonUnchecked, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = option.feedName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                    )
                    if (option.isMain) {
                        Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(0.15f)) {
                            Text(
                                text     = "MAIN",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = catColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (option.languageNames.isNotEmpty()) {
                    Text(
                        text  = "🌐 " + option.languageNames.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quality badge
            option.quality?.let { q ->
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text     = q,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
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
    onWidget: () -> Unit,
    onSelectStream: (Int) -> Unit
) {
    val context   = LocalContext.current
    val activity  = context as Activity
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val catColor  = categoryColorFor(channel)

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol       = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var isPlaying       by remember { mutableStateOf(true) }
    var playerError     by remember { mutableStateOf<String?>(null) }
    var isLocked        by remember { mutableStateOf(false) }
    var showControls    by remember { mutableStateOf(true) }
    var isRotLocked     by remember { mutableStateOf(false) }
    var showFeedPicker  by remember { mutableStateOf(false) }

    var volumeLevel by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }
    var brightnessLevel by remember {
        val cur = activity.window.attributes.screenBrightness
        mutableStateOf(if (cur < 0) 0.5f else cur)
    }

    fun applyBrightness(value: Float) {
        val clamped = value.coerceIn(0.01f, 1.0f)
        val lp = activity.window.attributes
        lp.screenBrightness = clamped
        activity.window.attributes = lp
    }

    fun applyVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (clamped * maxVol).roundToInt().coerceIn(0, maxVol.roundToInt()),
            0
        )
    }

    LaunchedEffect(Unit) {
        // Ensure audio is not muted when entering fullscreen
        if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
        // If volume is 0, set it to 30% so there's always sound
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol == 0) {
            val thirtyPct = (maxVol * 0.3f).roundToInt().coerceAtLeast(1)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, thirtyPct, 0)
            volumeLevel = thirtyPct / maxVol
        }

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

    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val lp = activity.window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = lp
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
        // Ensure player audio is not muted
        exoPlayer?.volume = 1f
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
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        val delta  = -dragAmount / size.height.toFloat()
                        val isLeft = change.position.x < size.width / 2f
                        if (isLeft) {
                            val newBright = (brightnessLevel + delta).coerceIn(0.01f, 1.0f)
                            brightnessLevel = newBright
                            applyBrightness(newBright)
                        } else {
                            val newVol = (volumeLevel + delta).coerceIn(0f, 1f)
                            // Never allow dragging to true 0 — keep at least 5%
                            val safeVol = newVol.coerceAtLeast(0.05f)
                            volumeLevel = safeVol
                            applyVolume(safeVol)
                        }
                        showControls = true
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        showControls = !showControls
                    }
                }
            }
    ) {
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

        EdgeLevelBars(brightnessLevel = brightnessLevel, volumeLevel = volumeLevel)

        if (isLocked) {
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
                    },
                    onOpenFeedPicker = if (channel.hasMultipleFeeds) {{ showFeedPicker = true }} else null
                )
            }
            playerError?.let { err ->
                ErrorOverlay(err) { playerError = null; exoPlayer?.prepare(); exoPlayer?.play() }
            }
        }
    }

    if (showFeedPicker) {
        FeedPickerSheet(
            channel   = channel,
            catColor  = catColor,
            onSelect  = { idx ->
                onSelectStream(idx)
                showFeedPicker = false
            },
            onDismiss = { showFeedPicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fullscreen controls (updated with feed picker button)
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
    onToggleRot: () -> Unit,
    onOpenFeedPicker: (() -> Unit)?
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                // Show current feed name if multiple feeds
                channel.currentStream?.let { stream ->
                    if (channel.hasMultipleFeeds) {
                        Text(
                            text  = stream.feedName + if (stream.languageNames.isNotEmpty()) " · ${stream.languageNames.first()}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.7f)
                        )
                    }
                }
            }
            LiveBadge()
            Spacer(Modifier.width(8.dp))

            // Feed/Language picker button
            if (onOpenFeedPicker != null) {
                IconButton(onClick = onOpenFeedPicker) {
                    Icon(Icons.Filled.Language, "Change Feed", tint = catColor)
                }
            }
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
                    imageVector        = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onForward, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Text(
            text     = "← Brightness  |  Volume →",
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White.copy(0.45f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edge level bars
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EdgeLevelBars(brightnessLevel: Float, volumeLevel: Float) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxHeight().width(5.dp).align(Alignment.CenterStart).padding(vertical = 60.dp)
        ) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxWidth().fillMaxHeight(brightnessLevel.coerceIn(0.01f, 1f)).align(Alignment.BottomStart).background(Color(0xFFFBD38D).copy(0.85f), RoundedCornerShape(3.dp)))
        }
        Box(
            modifier = Modifier.fillMaxHeight().width(5.dp).align(Alignment.CenterEnd).padding(vertical = 60.dp)
        ) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(0.12f), RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxWidth().fillMaxHeight(volumeLevel.coerceIn(0.05f, 1f)).align(Alignment.BottomStart).background(Color(0xFF4F8EF7).copy(0.85f), RoundedCornerShape(3.dp)))
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

// ─────────────────────────────────────────────────────────────────────────────
// ExoPlayer factory — always with audio enabled, rebuilds on URL change
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
        // Guarantee audio is never muted at the player level
        volume = 1f
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
