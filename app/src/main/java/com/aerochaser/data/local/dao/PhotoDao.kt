package com.aerochaser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aerochaser.data.local.entity.ExifDataEntity
import com.aerochaser.data.local.entity.PhotoEntity
import com.aerochaser.data.local.entity.PhotoWithExif
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExifData(exifData: ExifDataEntity)

    @Transaction
    open suspend fun insertPhotoWithExif(photo: PhotoEntity, exifData: ExifDataEntity?) {
        insertPhoto(photo)
        if (exifData != null) {
            insertExifData(exifData)
        }
    }

    @Transaction
    @Query("SELECT * FROM photos ORDER BY captureDateMs DESC")
    abstract fun getAllPhotos(): Flow<List<PhotoWithExif>>

    @Transaction
    @Query("SELECT * FROM photos ORDER BY captureDateMs DESC")
    abstract suspend fun getAllPhotosSync(): List<PhotoWithExif>

    @Transaction
    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    abstract suspend fun getPhotoById(id: String): PhotoWithExif?
}
