package com.aerochaser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val localUri: String,
    val captureDateMs: Long,
    val importedAtMs: Long,
    // Cloud metadata fields
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val modifiedDateMs: Long? = null,
    val thumbnailUrl: String? = null
)
