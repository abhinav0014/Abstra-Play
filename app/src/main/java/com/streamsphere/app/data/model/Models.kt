package com.streamsphere.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class Channel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("alt_names") val altNames: List<String> = emptyList(),
    @SerializedName("network") val network: String? = null,
    @SerializedName("owners") val owners: List<String> = emptyList(),
    @SerializedName("country") val country: String,
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("is_nsfw") val isNsfw: Boolean = false,
    @SerializedName("launched") val launched: String? = null,
    @SerializedName("closed") val closed: String? = null,
    @SerializedName("replaced_by") val replacedBy: String? = null,
    @SerializedName("website") val website: String? = null
)

data class Stream(
    @SerializedName("channel")    val channel: String? = null,
    @SerializedName("feed")       val feed: String? = null,
    @SerializedName("title")      val title: String,
    @SerializedName("url")        val url: String,
    @SerializedName("referrer")   val referrer: String? = null,
    @SerializedName("user_agent") val userAgent: String? = null,
    @SerializedName("quality")    val quality: String? = null
)

data class Logo(
    @SerializedName("channel") val channel: String,
    @SerializedName("feed")    val feed: String? = null,
    @SerializedName("tags")    val tags: List<String> = emptyList(),
    @SerializedName("width")   val width: Double = 0.0,
    @SerializedName("height")  val height: Double = 0.0,
    @SerializedName("format")  val format: String? = null,
    @SerializedName("url")     val url: String
)

data class Category(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String
)

data class Country(
    @SerializedName("name")      val name: String,
    @SerializedName("code")      val code: String,
    @SerializedName("languages") val languages: List<String> = emptyList(),
    @SerializedName("flag")      val flag: String
)

data class Feed(
    @SerializedName("channel")        val channel: String,
    @SerializedName("id")             val id: String,
    @SerializedName("name")           val name: String,
    @SerializedName("alt_names")      val altNames: List<String> = emptyList(),
    @SerializedName("is_main")        val isMain: Boolean = false,
    @SerializedName("broadcast_area") val broadcastArea: List<String> = emptyList(),
    @SerializedName("timezones")      val timezones: List<String> = emptyList(),
    @SerializedName("languages")      val languages: List<String> = emptyList(),
    @SerializedName("format")         val format: String? = null
)

data class Language(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String   // ISO 639-3
)

/** One selectable stream entry shown in the feed/language picker. */
data class StreamOption(
    val feedId: String?,
    val feedName: String,
    val languages: List<String>,       // ISO 639-3 codes
    val languageNames: List<String>,   // human-readable
    val quality: String?,
    val url: String,
    val referrer: String?,
    val userAgent: String?,
    val isMain: Boolean
)

@Entity(tableName = "favourites")
data class FavouriteChannel(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val categories: String,
    val logoUrl: String? = null,
    val streamUrl: String? = null,
    val isWidget: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

data class ChannelUiModel(
    val id: String,
    val name: String,
    val country: String,
    val countryFlag: String,
    val categories: List<String>,
    val logoUrl: String?,
    /** All available stream options (feeds) for this channel. */
    val streamOptions: List<StreamOption>,
    /** Index into streamOptions that is currently active. */
    val selectedStreamIndex: Int = 0,
    val isFavourite: Boolean = false,
    val isWidget: Boolean = false
) {
    val streamUrl: String?
        get() = streamOptions.getOrNull(selectedStreamIndex)?.url
    val currentStream: StreamOption?
        get() = streamOptions.getOrNull(selectedStreamIndex)
    val hasMultipleFeeds: Boolean
        get() = streamOptions.size > 1
}

enum class ChannelTab(val label: String, val emoji: String, val countryCodes: List<String>, val categories: List<String>) {
    ALL("All",      "🌐", emptyList(), emptyList()),
    NEPAL("Nepal",  "🇳🇵", listOf("NP"), emptyList()),
    INDIA("India",  "🇮🇳", listOf("IN"), emptyList()),
    SCIENCE("Science", "🔬", emptyList(), listOf("science", "education", "kids")),
    MUSIC("Music",  "🎵", emptyList(), listOf("music", "entertainment"));
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
