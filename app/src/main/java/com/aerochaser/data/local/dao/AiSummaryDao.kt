package com.aerochaser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aerochaser.data.local.entity.AiSummaryEntity

@Dao
interface AiSummaryDao {

    @Query("SELECT * FROM ai_summaries WHERE gearKey = :gearKey LIMIT 1")
    suspend fun getSummary(gearKey: String): AiSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: AiSummaryEntity)
}
