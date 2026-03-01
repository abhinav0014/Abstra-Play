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
    private var cachedChannels:  List<Channel>?  = null
    private var cachedStreams:   List<Stream>?   = null
    private var cachedLogos:     List<Logo>?     = null
    private var cachedCountries: List<Country>?  = null
    private var cachedFeeds:     List<Feed>?     = null
    private var cachedLanguages: List<Language>? = null

    suspend fun getChannels():  List<Channel>  = cachedChannels  ?: api.getChannels().also  { cachedChannels  = it }
    suspend fun getStreams():   List<Stream>   = cachedStreams   ?: api.getStreams().also   { cachedStreams   = it }
    suspend fun getLogos():     List<Logo>     = cachedLogos     ?: api.getLogos().also    { cachedLogos     = it }
    suspend fun getCountries(): List<Country>  = cachedCountries ?: api.getCountries().also{ cachedCountries = it }
    suspend fun getFeeds():     List<Feed>     = cachedFeeds     ?: api.getFeeds().also    { cachedFeeds     = it }
    suspend fun getLanguages(): List<Language> = cachedLanguages ?: api.getLanguages().also{ cachedLanguages = it }

    suspend fun buildChannelUiModels(
        favouriteIds: Set<String> = emptySet(),
        widgetIds:    Set<String> = emptySet()
    ): List<ChannelUiModel> {
        val channels  = getChannels()
        val allStreams = getStreams()
        val logos     = getLogos().groupBy { it.channel }
        val countries = getCountries().associateBy { it.code }
        // feeds keyed by (channelId, feedId)
        val feedMap   = getFeeds().groupBy { it.channel }
        // languages keyed by ISO 639-3 code
        val langMap   = getLanguages().associateBy { it.code }

        // streams grouped by channel id
        val streamsByChannel = allStreams.groupBy { it.channel }

        val targetCountries  = setOf("NP", "IN")
        val targetCategories = setOf("science", "education", "music", "entertainment", "kids")

        return channels
            .filter { ch ->
                !ch.isNsfw && ch.closed == null &&
                streamsByChannel.containsKey(ch.id) &&      // only live channels
                (ch.country in targetCountries || ch.categories.any { it in targetCategories })
            }
            .map { ch ->
                val country     = countries[ch.country]
                val logo        = logos[ch.id]?.firstOrNull()
                val chStreams    = streamsByChannel[ch.id] ?: emptyList()
                val chFeeds     = feedMap[ch.id]?.associateBy { it.id } ?: emptyMap()

                // Build StreamOption list, sorted: main feed first, then by feed name
                val options = chStreams
                    .map { stream ->
                        val feed = stream.feed?.let { chFeeds[it] }
                        val langs = feed?.languages ?: emptyList()
                        StreamOption(
                            feedId        = stream.feed,
                            feedName      = feed?.name ?: stream.title,
                            languages     = langs,
                            languageNames = langs.mapNotNull { langMap[it]?.name },
                            quality       = stream.quality,
                            url           = stream.url,
                            referrer      = stream.referrer,
                            userAgent     = stream.userAgent,
                            isMain        = feed?.isMain ?: (stream.feed == null)
                        )
                    }
                    // main / no-feed first, then alphabetically by feedName
                    .sortedWith(compareByDescending<StreamOption> { it.isMain }.thenBy { it.feedName })
                    .distinctBy { it.url }   // deduplicate identical URLs

                ChannelUiModel(
                    id                  = ch.id,
                    name                = ch.name,
                    country             = country?.name ?: ch.country,
                    countryFlag         = country?.flag ?: "🌐",
                    categories          = ch.categories,
                    logoUrl             = logo?.url,
                    streamOptions       = options,
                    selectedStreamIndex = 0,
                    isFavourite         = ch.id in favouriteIds,
                    isWidget            = ch.id in widgetIds
                )
            }
            .distinctBy { it.id }
    }

    // ── Favourites ────────────────────────────────────────────────────────────

    fun getAllFavourites():   Flow<List<FavouriteChannel>> = db.favouritesDao().getAllFavourites()
    fun getWidgetChannels():  Flow<List<FavouriteChannel>> = db.favouritesDao().getWidgetChannels()
    fun isFavourite(id: String): Flow<Boolean>            = db.favouritesDao().isFavourite(id)

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
