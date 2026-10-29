package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autotech.android.data.local.entity.PendingMaintenanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMaintenanceDao {
    /** Marcar de novo o mesmo item substitui a pendência anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PendingMaintenanceEntity): Long

    @Query(
        "SELECT p.* FROM pending_maintenance p INNER JOIN vehicles v ON v.id = p.vehicleId " +
            "WHERE v.userId = :userId AND v.archived = 0 ORDER BY p.doneDateMillis DESC"
    )
    fun observeByUser(userId: Long): Flow<List<PendingMaintenanceEntity>>

    @Query("SELECT * FROM pending_maintenance WHERE vehicleId = :vehicleId ORDER BY doneDateMillis DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<PendingMaintenanceEntity>>

    @Query("SELECT * FROM pending_maintenance WHERE vehicleId = :vehicleId")
    suspend fun listByVehicle(vehicleId: Long): List<PendingMaintenanceEntity>

    @Query("SELECT * FROM pending_maintenance WHERE id = :id")
    suspend fun getById(id: Long): PendingMaintenanceEntity?

    @Query("DELETE FROM pending_maintenance WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_maintenance WHERE vehicleId = :vehicleId AND itemKey = :itemKey")
    suspend fun deleteByItem(vehicleId: Long, itemKey: String)
}
