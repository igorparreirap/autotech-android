package com.autotech.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val itemKey: String,
    /** RF-04: data em que o serviço foi executado (usada em ciclos, painel e relatório). */
    val eventDateMillis: Long,
    val eventKm: Long,
    val vehicleKmAtRecord: Long,
    val amount: Double?,
    val description: String?,
    val receiptPath: String?,
    /** RF-04: data em que o registro foi lançado no app. */
    @ColumnInfo(defaultValue = "0") val registeredAtMillis: Long = 0,
    /** RF-04: parte do valor que foi mão de obra (só quando o usuário separa; [amount] continua sendo o total). */
    val laborAmount: Double? = null
)
