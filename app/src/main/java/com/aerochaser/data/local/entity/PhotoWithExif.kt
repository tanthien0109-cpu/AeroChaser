package com.aerochaser.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PhotoWithExif(
    @Embedded val photo: PhotoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "photoId"
    )
    val exifData: ExifDataEntity?
)
