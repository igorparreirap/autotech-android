package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insert(record: MaintenanceRecordEntity): Long

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY eventDateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId ORDER BY eventDateMillis DESC")
    suspend fun listByVehicle(vehicleId: Long): List<MaintenanceRecordEntity>

    @Query("SELECT MAX(eventKm) FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun maxEventKm(vehicleId: Long): Long?

    @Query("SELECT MAX(vehicleKmAtRecord) FROM maintenance_records WHERE vehicleId = :vehicleId")
    suspend fun maxVehicleKmAtRecord(vehicleId: Long): Long?
}
