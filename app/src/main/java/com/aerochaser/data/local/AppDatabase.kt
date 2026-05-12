package com.aerochaser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aerochaser.data.local.dao.AiSummaryDao
import com.aerochaser.data.local.dao.PhotoDao
import com.aerochaser.data.local.entity.AiSummaryEntity
import com.aerochaser.data.local.entity.ExifDataEntity
import com.aerochaser.data.local.entity.PhotoEntity

@Database(
    entities = [PhotoEntity::class, ExifDataEntity::class, AiSummaryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun aiSummaryDao(): AiSummaryDao
}
