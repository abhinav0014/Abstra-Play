package com.streamsphere.app.data.repository

import com.streamsphere.app.data.api.AppDatabase
import com.streamsphere.app.data.api.IptvApi
import com.streamsphere.app.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val api: IptvApi,
    private val db: AppDatabase
) {
    // ── Remote ────────────────────────────────────────────────────────────────

    private var cachedChannels: List<Channel>? = null
    private var cachedStreams: List<Stream>? = null
    private var cachedLogos: List<Logo>? = null
    private var cachedCountries: List<Country>? = null

    suspend fun getChannels(): List<Channel> =
        cachedChannels ?: api.getChannels().also { cachedChannels = it }

    suspend fun getStreams(): List<Stream> =
        cachedStreams ?: api.getStreams().also { cachedStreams = it }

    suspend fun getLogos(): List<Logo> =
        cachedLogos ?: api.getLogos().also { cachedLogos = it }

    suspend fun getCountries(): List<Country> =
        cachedCountries ?: api.getCountries().also { cachedCountries = it }

    suspend fun buildChannelUiModels(
        favouriteIds: Set<String> = emptySet(),
        widgetIds: Set<String> = emptySet()
    ): List<ChannelUiModel> {
        val channels = getChannels()
        val streams   = getStreams().groupBy { it.channel }
        val logos     = getLogos().groupBy { it.channel }
        val countries = getCountries().associateBy { it.code }

        // Filter to target countries + science/music categories only
        val targetCountries = setOf("NP", "IN")
        val targetCategories = setOf("science", "education", "music", "entertainment", "kids")

        return channels
            .filter { ch ->
                !ch.isNsfw &&
                ch.closed == null &&
                (ch.country in targetCountries ||
                 ch.categories.any { it in targetCategories })
            }
            .map { ch ->
                val country = countries[ch.country]
                val logo    = logos[ch.id]?.firstOrNull()
                val stream  = streams[ch.id]?.firstOrNull()
                ChannelUiModel(
                    id          = ch.id,
                    name        = ch.name,
                    country     = country?.name ?: ch.country,
                    countryFlag = country?.flag ?: "🌐",
                    categories  = ch.categories,
                    logoUrl     = logo?.url,
                    streamUrl   = stream?.url,
                    isFavourite = ch.id in favouriteIds,
                    isWidget    = ch.id in widgetIds
                )
            }
            .distinctBy { it.id }
    }

    // ── Local (Room) ──────────────────────────────────────────────────────────

    fun getAllFavourites(): Flow<List<FavouriteChannel>> =
        db.favouritesDao().getAllFavourites()

    fun getWidgetChannels(): Flow<List<FavouriteChannel>> =
        db.favouritesDao().getWidgetChannels()

    fun isFavourite(id: String): Flow<Boolean> =
        db.favouritesDao().isFavourite(id)

    suspend fun toggleFavourite(model: ChannelUiModel) {
        if (model.isFavourite) {
            db.favouritesDao().deleteById(model.id)
        } else {
            db.favouritesDao().insertFavourite(
                FavouriteChannel(
                    id         = model.id,
                    name       = model.name,
                    country    = model.country,
                    categories = model.categories.joinToString(","),
                    logoUrl    = model.logoUrl,
                    streamUrl  = model.streamUrl
                )
            )
        }
    }

    suspend fun toggleWidget(id: String, current: Boolean) {
        db.favouritesDao().updateWidgetStatus(id, !current)
    }
}
