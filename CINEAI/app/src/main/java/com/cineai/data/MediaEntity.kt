package com.cineai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaKind { PHOTO, VIDEO }
enum class ProcessingStatus { IMPORTED, PROCESSING, READY, FAILED }

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: String,
    val originalPath: String,
    val enhancedPath: String? = null,
    val displayName: String,
    val mimeType: String,
    val kind: String,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val sizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = ProcessingStatus.IMPORTED.name,
    val preset: String = "AUTO AI",
    val profileJson: String = ""
)
