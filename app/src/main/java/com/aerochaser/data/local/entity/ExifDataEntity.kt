package com.aerochaser.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exif_data",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExifDataEntity(
    @PrimaryKey val photoId: String,
    val cameraModel: String?,
    val lensModel: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val iso: Int?,
    val focalLength: String?,
    val gpsLat: Double?,
    val gpsLng: Double?
)
