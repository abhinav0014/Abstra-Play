package com.streamsphere.app.data.api

import androidx.room.*
import com.streamsphere.app.data.model.FavouriteChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouritesDao {
    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllFavourites(): Flow<List<FavouriteChannel>>

    @Query("SELECT * FROM favourites WHERE isWidget = 1 ORDER BY addedAt DESC LIMIT 4")
    fun getWidgetChannels(): Flow<List<FavouriteChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(channel: FavouriteChannel)

    @Delete
    suspend fun deleteFavourite(channel: FavouriteChannel)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE id = :id)")
    fun isFavourite(id: String): Flow<Boolean>

    @Query("UPDATE favourites SET isWidget = :isWidget WHERE id = :id")
    suspend fun updateWidgetStatus(id: String, isWidget: Boolean)

    @Query("DELETE FROM favourites WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [FavouriteChannel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouritesDao(): FavouritesDao
}
