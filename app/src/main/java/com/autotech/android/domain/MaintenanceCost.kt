package com.autotech.android.domain

/**
 * RF-04: valor de uma manutenção, com a mão de obra separada quando o usuário quiser.
 * O valor total continua sendo a base do painel financeiro e do relatório.
 */
object MaintenanceCost {

    /** Total = peças + mão de obra; null se nenhum dos dois foi informado. */
    fun total(parts: Double?, labor: Double?): Double? =
        if (parts == null && labor == null) null else (parts ?: 0.0) + (labor ?: 0.0)

    /** Valor das peças a partir do total gravado e da mão de obra (null se a mão de obra não foi separada). */
    fun parts(total: Double?, labor: Double?): Double? =
        if (total == null || labor == null) null else (total - labor).coerceAtLeast(0.0)
}
