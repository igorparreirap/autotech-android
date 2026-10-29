package com.autotech.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RF02: cache local das placas já consultadas.
 * Evita repetir (e pagar) a mesma consulta na SimplesAPI e permite
 * recuperar os dados sem internet depois da primeira busca.
 */
@Entity(tableName = "plate_lookup_cache")
data class PlateLookupCacheEntity(
    @PrimaryKey val plate: String,
    val brand: String,
    val model: String,
    val version: String?,
    val year: Int?,
    val modelYear: Int?,
    val color: String?,
    val fuel: String?,
    val fetchedAtMillis: Long
)
