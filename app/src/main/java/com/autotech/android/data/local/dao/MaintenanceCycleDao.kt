package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceCycleDao {
    @Insert
    suspend fun insert(cycle: MaintenanceCycleEntity): Long

    @Query("SELECT * FROM maintenance_cycles WHERE vehicleId = :vehicleId")
    fun observeByVehicle(vehicleId: Long): Flow<List<MaintenanceCycleEntity>>
}
