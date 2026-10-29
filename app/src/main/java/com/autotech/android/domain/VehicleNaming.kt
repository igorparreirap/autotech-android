package com.autotech.android.domain

/** RF-03: regras do nome do veículo (apelido x nome original da consulta da placa). */
object VehicleNaming {

    const val MAX_LENGTH = 40

    fun original(brand: String, model: String): String = "$brand $model".trim()

    fun display(nickname: String?, brand: String, model: String): String =
        nickname?.takeIf { it.isNotBlank() } ?: original(brand, model)

    fun short(nickname: String?, model: String): String =
        nickname?.takeIf { it.isNotBlank() } ?: model

    /** Nome vazio ou igual ao original = sem apelido (o veículo volta ao nome original). */
    fun normalizeNickname(input: String?, brand: String, model: String): String? =
        input?.trim()?.take(MAX_LENGTH)
            ?.takeIf { it.isNotBlank() && !it.equals(original(brand, model), ignoreCase = true) }
}
