package com.streamsphere.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamsphere.app.data.model.*
import com.streamsphere.app.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val repo: ChannelRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<List<ChannelUiModel>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ChannelUiModel>>> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(ChannelTab.ALL)
    val selectedTab: StateFlow<ChannelTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val favourites: StateFlow<List<FavouriteChannel>> =
        repo.getAllFavourites()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val widgetChannels: StateFlow<List<FavouriteChannel>> =
        repo.getWidgetChannels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Derived filtered list
    val filteredChannels: StateFlow<List<ChannelUiModel>> =
        combine(_uiState, _selectedTab, _searchQuery, favourites, widgetChannels) {
            state, tab, query, favs, widgets ->
            val favIds    = favs.map { it.id }.toSet()
            val widgetIds = widgets.map { it.id }.toSet()
            val all = (state as? UiState.Success)?.data ?: return@combine emptyList()
            all.map { ch ->
                ch.copy(isFavourite = ch.id in favIds, isWidget = ch.id in widgetIds)
            }.filter { ch ->
                val matchesTab = when (tab) {
                    ChannelTab.ALL     -> true
                    ChannelTab.NEPAL   -> ch.country == "Nepal" || ch.country == "NP"
                    ChannelTab.INDIA   -> ch.country == "India" || ch.country == "IN"
                    ChannelTab.SCIENCE -> ch.categories.any { it in listOf("science","education","kids") }
                    ChannelTab.MUSIC   -> ch.categories.any { it in listOf("music","entertainment") }
                }
                val matchesQuery = query.isBlank() ||
                    ch.name.contains(query, ignoreCase = true) ||
                    ch.country.contains(query, ignoreCase = true)
                matchesTab && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Init ──────────────────────────────────────────────────────────────────

    init { loadChannels() }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                val models = repo.buildChannelUiModels()
                _uiState.value = UiState.Success(models)
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Unknown error")
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun selectTab(tab: ChannelTab) { _selectedTab.value = tab }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun toggleFavourite(model: ChannelUiModel) {
        viewModelScope.launch { repo.toggleFavourite(model) }
    }

    fun toggleWidget(model: ChannelUiModel) {
        viewModelScope.launch {
            if (!model.isFavourite) {
                // Must be favourite first
                repo.toggleFavourite(model)
            }
            repo.toggleWidget(model.id, model.isWidget)
        }
    }
}
