package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autotech.android.data.local.entity.FuelRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Insert
    suspend fun insert(record: FuelRecordEntity): Long

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records WHERE vehicleId = :vehicleId ORDER BY dateMillis DESC")
    suspend fun listByVehicle(vehicleId: Long): List<FuelRecordEntity>

    @Query("SELECT MAX(eventKm) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun maxEventKm(vehicleId: Long): Long?

    @Query("SELECT MAX(vehicleKm) FROM fuel_records WHERE vehicleId = :vehicleId")
    suspend fun maxVehicleKm(vehicleId: Long): Long?
}
