package com.streamsphere.app.data.api

import com.streamsphere.app.data.model.*
import retrofit2.http.GET

interface IptvApi {
    @GET("channels.json")   suspend fun getChannels(): List<Channel>
    @GET("streams.json")    suspend fun getStreams(): List<Stream>
    @GET("logos.json")      suspend fun getLogos(): List<Logo>
    @GET("categories.json") suspend fun getCategories(): List<Category>
    @GET("countries.json")  suspend fun getCountries(): List<Country>
    @GET("feeds.json")      suspend fun getFeeds(): List<Feed>
    @GET("languages.json")  suspend fun getLanguages(): List<Language>
}
