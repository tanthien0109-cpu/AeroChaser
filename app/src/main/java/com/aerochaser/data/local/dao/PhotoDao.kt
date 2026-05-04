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
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExifData(exifData: ExifDataEntity)

    @Transaction
    suspend fun insertPhotoWithExif(photo: PhotoEntity, exifData: ExifDataEntity?) {
        insertPhoto(photo)
        if (exifData != null) {
            insertExifData(exifData)
        }
    }

    @Transaction
    @Query("SELECT * FROM photos ORDER BY captureDateMs DESC")
    fun getAllPhotos(): Flow<List<PhotoWithExif>>
    
    @Transaction
    @Query("SELECT * FROM photos ORDER BY captureDateMs DESC")
    suspend fun getAllPhotosSync(): List<PhotoWithExif>
}
