package com.autotech.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RF11 / RN04: intervalo personalizado após um serviço; alertas só com ciclo definido.
 */
@Entity(
    tableName = "maintenance_cycles",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MaintenanceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedMaintenanceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("linkedMaintenanceId")]
)
data class MaintenanceCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val linkedMaintenanceId: Long,
    val itemKey: String,
    val intervalKm: Int?,
    val intervalMonths: Int?
)
