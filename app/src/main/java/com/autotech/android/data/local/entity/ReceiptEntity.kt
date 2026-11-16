package com.autotech.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RF-04 / RF-05: um comprovante anexado (foto, PDF, XML...). Cada arquivo é uma linha, ligada à
 * manutenção ou ao abastecimento a que pertence; apagar o registro apaga as linhas em cascata.
 * Exatamente um dos dois vínculos é preenchido.
 */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = MaintenanceRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["maintenanceRecordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FuelRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["fuelRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("maintenanceRecordId"), Index("fuelRecordId")]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val maintenanceRecordId: Long?,
    val fuelRecordId: Long?,
    /** caminho do arquivo na pasta privada do app */
    val path: String,
    /** ordem em que o usuário anexou */
    val position: Int,
    val createdAtMillis: Long
)
