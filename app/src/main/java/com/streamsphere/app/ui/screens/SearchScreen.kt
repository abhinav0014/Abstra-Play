package com.streamsphere.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamsphere.app.data.model.ChannelTab
import com.streamsphere.app.data.model.ChannelUiModel
import com.streamsphere.app.ui.components.ChannelCard
import com.streamsphere.app.viewmodel.ChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onChannelClick: (String) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val query    by viewModel.searchQuery.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Auto-show all when no query
        viewModel.selectTab(ChannelTab.ALL)
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(
                query    = query,
                onChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
            )

            AnimatedContent(
                targetState = channels.isEmpty() && query.isNotBlank(),
                label       = "search_content"
            ) { noResults ->
                if (noResults) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text  = "No results for \"$query\"",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(340.dp),
                        contentPadding = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            bottom = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing   = 12.dp
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.id }) { _, ch ->
                            ChannelCard(
                                model            = ch,
                                onFavouriteClick = { viewModel.toggleFavourite(ch) },
                                onWidgetClick    = { viewModel.toggleWidget(ch) },
                                onCardClick      = { onChannelClick(ch.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
