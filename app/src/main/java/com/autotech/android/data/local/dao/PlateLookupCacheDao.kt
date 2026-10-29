package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autotech.android.data.local.entity.PlateLookupCacheEntity

@Dao
interface PlateLookupCacheDao {
    @Query("SELECT * FROM plate_lookup_cache WHERE plate = :plate LIMIT 1")
    suspend fun get(plate: String): PlateLookupCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PlateLookupCacheEntity)
}
