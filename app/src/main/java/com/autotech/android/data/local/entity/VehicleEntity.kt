package com.autotech.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        // RF03: o mesmo usuário não pode cadastrar dois veículos com a mesma placa.
        Index(value = ["userId", "plate"], unique = true)
    ]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val plate: String,
    val brand: String,
    val model: String,
    val version: String? = null,
    /** Ano de fabricação. */
    val year: Int?,
    val modelYear: Int? = null,
    val color: String? = null,
    val fuel: String? = null,
    val currentOdometerKm: Long,
    /** RF-03: veículo excluído da lista, mas com o histórico mantido. */
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    /** RF-03: nome personalizado; null = usa marca + modelo da consulta da placa. */
    val nickname: String? = null
)
