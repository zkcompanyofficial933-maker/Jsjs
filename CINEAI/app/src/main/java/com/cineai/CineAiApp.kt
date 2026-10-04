package com.cineai

import android.app.Application
import androidx.room.Room
import com.cineai.data.CineDatabase
import com.cineai.data.MediaRepository

class CineAiApp : Application() {
    lateinit var database: CineDatabase
    lateinit var repository: MediaRepository
    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, CineDatabase::class.java, "cineai.db").fallbackToDestructiveMigration().build()
        repository = MediaRepository(this, database.mediaDao())
    }
}
