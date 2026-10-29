package com.autotech.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_records",
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
data class FuelRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    /** RF-05: data do abastecimento (usada no painel e no relatório). */
    val dateMillis: Long,
    val eventKm: Long,
    val vehicleKm: Long,
    val liters: Double,
    val amount: Double?,
    val receiptPath: String?,
    /** RF-05: data em que o registro foi lançado no app. */
    @ColumnInfo(defaultValue = "0") val registeredAtMillis: Long = 0,
    /** RF-05: litros de álcool e de gasolina, só em veículo flex e só se o usuário especificar. */
    val ethanolLiters: Double? = null,
    val gasolineLiters: Double? = null,
    /** RF-05: o usuário pediu o cálculo de consumo (km/L) neste abastecimento. */
    @ColumnInfo(defaultValue = "0") val computeConsumption: Boolean = false
)
