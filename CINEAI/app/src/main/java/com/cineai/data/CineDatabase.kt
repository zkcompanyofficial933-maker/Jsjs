package com.cineai.data
import androidx.room.Database
import androidx.room.RoomDatabase
@Database(entities=[MediaEntity::class], version=1, exportSchema=false)
abstract class CineDatabase: RoomDatabase(){ abstract fun mediaDao(): MediaDao }
