package com.streamsphere.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamsphere.app.data.model.*
import com.streamsphere.app.ui.components.*
import com.streamsphere.app.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChannelClick: (String) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val channels    by viewModel.filteredChannels.collectAsState()

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullRefreshState     = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.loadChannels()
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            // LargeTopAppBar: title collapses on scroll; Refresh is in actions (top-right)
            LargeTopAppBar(
                title = {
                    // When expanded shows two lines; when collapsed shows just the app name
                    Column {
                        Text(
                            text  = "StreamSphere",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        // Only visible while expanded
                        Text(
                            text  = "Global TV Channels",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                // Refresh lives here — always visible in the top-right corner
                actions = {
                    IconButton(onClick = { viewModel.loadChannels() }) {
                        Icon(
                            imageVector        = Icons.Filled.Refresh,
                            contentDescription = "Refresh channels"
                        )
                    }
                },
                scrollBehavior = topBarScrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> LoadingContent()
                is UiState.Error   -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::loadChannels
                )
                is UiState.Success -> {
                    ChannelListContent(
                        channels      = channels,
                        selectedTab   = selectedTab,
                        searchQuery   = searchQuery,
                        allChannels   = state.data,
                        onTabSelected = viewModel::selectTab,
                        onSearchQuery = viewModel::setSearchQuery,
                        onFavourite   = viewModel::toggleFavourite,
                        onWidget      = viewModel::toggleWidget,
                        onCardClick   = onChannelClick
                    )
                }
            }

            PullToRefreshContainer(
                state    = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun ChannelListContent(
    channels: List<ChannelUiModel>,
    selectedTab: ChannelTab,
    searchQuery: String,
    allChannels: List<ChannelUiModel>,
    onTabSelected: (ChannelTab) -> Unit,
    onSearchQuery: (String) -> Unit,
    onFavourite: (ChannelUiModel) -> Unit,
    onWidget: (ChannelUiModel) -> Unit,
    onCardClick: (String) -> Unit
) {
    val counts = remember(allChannels) {
        ChannelTab.entries.associateWith { tab ->
            allChannels.count { ch ->
                when (tab) {
                    ChannelTab.ALL     -> true
                    ChannelTab.NEPAL   -> ch.country.contains("Nepal", ignoreCase = true)
                    ChannelTab.INDIA   -> ch.country.contains("India", ignoreCase = true)
                    ChannelTab.SCIENCE -> ch.categories.any { it in listOf("science", "education", "kids") }
                    ChannelTab.MUSIC   -> ch.categories.any { it in listOf("music", "entertainment") }
                }
            }
        }
    }

    LazyVerticalStaggeredGrid(
        columns               = StaggeredGridCells.Adaptive(340.dp),
        contentPadding        = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing   = 12.dp
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            SearchBar(
                query    = searchQuery,
                onChange = onSearchQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            CategoryTabRow(
                selectedTab   = selectedTab,
                counts        = counts,
                onTabSelected = onTabSelected,
                modifier      = Modifier.padding(bottom = 8.dp)
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                text     = "${channels.size} channels",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (channels.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { EmptyContent() }
        } else {
            itemsIndexed(
                items = channels,
                key   = { _, ch -> ch.id }
            ) { index, ch ->
                val animDelay = (index * 30).coerceAtMost(300)
                AnimatedVisibility(
                    visible  = true,
                    enter    = fadeIn(tween(300, delayMillis = animDelay)) +
                               slideInVertically(tween(300, delayMillis = animDelay)) { it / 3 },
                    modifier = Modifier.padding(
                        start = if (index % 2 == 0) 16.dp else 0.dp,
                        end   = if (index % 2 != 0) 16.dp else 0.dp
                    )
                ) {
                    ChannelCard(
                        model            = ch,
                        onFavouriteClick = { onFavourite(ch) },
                        onWidgetClick    = { onWidget(ch) },
                        onCardClick      = { onCardClick(ch.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value         = query,
        onValueChange = onChange,
        modifier      = modifier.fillMaxWidth(),
        placeholder   = {
            Text(
                text  = "Search channels, countries…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon  = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape      = MaterialTheme.shapes.large,
        colors     = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
        )
    )
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text  = "Loading channels…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Filled.WifiOff,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.error,
                modifier           = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text      = "Couldn't load channels",
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📺", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                text      = "No channels found",
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Try a different search or category",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
