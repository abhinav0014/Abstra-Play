package com.streamsphere.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamsphere.app.data.model.*
import com.streamsphere.app.data.repository.ChannelRepository
import com.streamsphere.app.util.ShortcutHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val repo: ChannelRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ChannelUiModel>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ChannelUiModel>>> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(ChannelTab.ALL)
    val selectedTab: StateFlow<ChannelTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _streamSelections = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** Emits a one-shot message to be shown as a Snackbar (null = nothing pending). */
    private val _shortcutMessage = MutableStateFlow<String?>(null)
    val shortcutMessage: StateFlow<String?> = _shortcutMessage.asStateFlow()

    val favourites: StateFlow<List<FavouriteChannel>> =
        repo.getAllFavourites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredChannels: StateFlow<List<ChannelUiModel>> =
        combine(
            combine(_uiState, _selectedTab, _searchQuery) { state, tab, query ->
                Triple(state, tab, query)
            },
            combine(favourites, _streamSelections) { favs, sels ->
                Pair(favs, sels)
            }
        ) { (state, tab, query), (favs, selections) ->
            val favIds = favs.map { it.id }.toSet()
            val all = (state as? UiState.Success<List<ChannelUiModel>>)?.data
                ?: return@combine emptyList()

            all.map { ch ->
                ch.copy(
                    isFavourite         = ch.id in favIds,
                    selectedStreamIndex = selections[ch.id] ?: 0
                )
            }.filter { ch ->
                val matchesTab = when (tab) {
                    ChannelTab.ALL     -> true
                    ChannelTab.NEPAL   -> ch.country == "Nepal" || ch.country == "NP"
                    ChannelTab.INDIA   -> ch.country == "India" || ch.country == "IN"
                    ChannelTab.SCIENCE -> ch.categories.any { it in listOf("science", "education", "kids") }
                    ChannelTab.MUSIC   -> ch.categories.any { it in listOf("music", "entertainment") }
                }
                val matchesQuery = query.isBlank() ||
                    ch.name.contains(query, ignoreCase = true) ||
                    ch.country.contains(query, ignoreCase = true)
                matchesTab && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { loadChannels() }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                _uiState.value = UiState.Success(repo.buildChannelUiModels())
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun selectTab(tab: ChannelTab)  { _selectedTab.value  = tab }
    fun setSearchQuery(q: String)   { _searchQuery.value  = q   }

    fun selectStream(channelId: String, index: Int) {
        _streamSelections.value = _streamSelections.value + (channelId to index)
    }

    fun toggleFavourite(model: ChannelUiModel) {
        viewModelScope.launch { repo.toggleFavourite(model) }
    }

    /**
     * Requests the launcher to pin a home-screen shortcut for [model].
     * The shortcut launches the fullscreen player directly.
     */
    fun pinShortcut(model: ChannelUiModel) {
        viewModelScope.launch {
            val supported = ShortcutHelper.pinChannelShortcut(appContext, model)
            _shortcutMessage.value = if (supported) {
                "Shortcut request sent — check your launcher prompt"
            } else {
                "Your launcher doesn't support pinned shortcuts"
            }
        }
    }

    /** Call after the Snackbar has been shown to clear the pending message. */
    fun onShortcutMessageConsumed() { _shortcutMessage.value = null }
}
