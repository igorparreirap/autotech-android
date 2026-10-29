package com.autotech.android.domain

/**
 * RF02: dados do veículo obtidos a partir da placa.
 * Modelo de domínio, independente da API externa que forneceu os dados.
 */
data class PlateVehicleData(
    val plate: String,
    val brand: String,
    val model: String,
    val version: String?,
    val year: Int?,
    val modelYear: Int?,
    val color: String?,
    val fuel: String?
)
