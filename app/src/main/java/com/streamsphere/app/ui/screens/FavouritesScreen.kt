package com.streamsphere.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamsphere.app.data.model.*
import com.streamsphere.app.ui.components.*
import com.streamsphere.app.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onChannelClick: (String) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val favourites  by viewModel.favourites.collectAsState()
    val allChannels by viewModel.filteredChannels.collectAsState()

    // Snackbar for shortcut feedback
    val shortcutMessage   by viewModel.shortcutMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(shortcutMessage) {
        shortcutMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onShortcutMessageConsumed()
        }
    }

    val favModels = remember(favourites, allChannels) {
        favourites.mapNotNull { fav ->
            allChannels.find { it.id == fav.id } ?: run {
                val options = if (fav.streamUrl != null) {
                    listOf(
                        StreamOption(
                            feedId        = null,
                            feedName      = fav.name,
                            languages     = emptyList(),
                            languageNames = emptyList(),
                            quality       = null,
                            url           = fav.streamUrl,
                            referrer      = null,
                            userAgent     = null,
                            isMain        = true
                        )
                    )
                } else emptyList()

                ChannelUiModel(
                    id                  = fav.id,
                    name                = fav.name,
                    country             = fav.country,
                    countryFlag         = "🌐",
                    categories          = fav.categories.split(",").filter { it.isNotBlank() },
                    logoUrl             = fav.logoUrl,
                    streamOptions       = options,
                    selectedStreamIndex = 0,
                    isFavourite         = true
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Favourites", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${favModels.size} channels saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (favModels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("💜", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No favourites yet", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the heart icon on any channel to save it here.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favModels, key = { it.id }) { model ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically()
                    ) {
                        ChannelCard(
                            model            = model,
                            onFavouriteClick = { viewModel.toggleFavourite(model) },
                            onPinShortcut    = { viewModel.pinShortcut(model) },
                            onCardClick      = { onChannelClick(model.id) }
                        )
                    }
                }
            }
        }
    }
}
