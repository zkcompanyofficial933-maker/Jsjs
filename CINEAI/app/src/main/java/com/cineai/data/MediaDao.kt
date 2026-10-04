package com.cineai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface MediaDao {
    @Query("SELECT * FROM media ORDER BY createdAt DESC") fun observeAll(): Flow<List<MediaEntity>>
    @Query("SELECT * FROM media WHERE id = :id LIMIT 1") suspend fun get(id: String): MediaEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: MediaEntity)
    @Delete suspend fun delete(item: MediaEntity)
    @Query("UPDATE media SET enhancedPath=:path, status=:status, profileJson=:profile, preset=:preset WHERE id=:id") suspend fun markEnhanced(id:String,path:String,status:String,profile:String,preset:String)
    @Query("UPDATE media SET status=:status WHERE id=:id") suspend fun setStatus(id:String,status:String)
}
