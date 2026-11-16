package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autotech.android.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insertAll(receipts: List<ReceiptEntity>)

    /** Comprovantes das manutenções de um veículo, na ordem em que foram anexados. */
    @Query(
        "SELECT r.* FROM receipts r JOIN maintenance_records m ON r.maintenanceRecordId = m.id " +
            "WHERE m.vehicleId = :vehicleId ORDER BY r.maintenanceRecordId, r.position"
    )
    fun observeMaintenanceByVehicle(vehicleId: Long): Flow<List<ReceiptEntity>>

    @Query(
        "SELECT r.* FROM receipts r JOIN maintenance_records m ON r.maintenanceRecordId = m.id " +
            "WHERE m.vehicleId = :vehicleId ORDER BY r.maintenanceRecordId, r.position"
    )
    suspend fun listMaintenanceByVehicle(vehicleId: Long): List<ReceiptEntity>

    /** Comprovantes dos abastecimentos de um veículo, na ordem em que foram anexados. */
    @Query(
        "SELECT r.* FROM receipts r JOIN fuel_records f ON r.fuelRecordId = f.id " +
            "WHERE f.vehicleId = :vehicleId ORDER BY r.fuelRecordId, r.position"
    )
    fun observeFuelByVehicle(vehicleId: Long): Flow<List<ReceiptEntity>>

    @Query(
        "SELECT r.* FROM receipts r JOIN fuel_records f ON r.fuelRecordId = f.id " +
            "WHERE f.vehicleId = :vehicleId ORDER BY r.fuelRecordId, r.position"
    )
    suspend fun listFuelByVehicle(vehicleId: Long): List<ReceiptEntity>
}
