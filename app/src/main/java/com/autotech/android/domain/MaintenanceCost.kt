package com.autotech.android.domain

/**
 * RF-04: valor de uma manutenção, com a mão de obra separada quando o usuário quiser.
 * Os valores são em centavos (ver [Money]). O total continua sendo a base do painel e do relatório.
 */
object MaintenanceCost {

    /** Total = peças + mão de obra; null se nenhum dos dois foi informado. */
    fun total(parts: Long?, labor: Long?): Long? =
        if (parts == null && labor == null) null else (parts ?: 0L) + (labor ?: 0L)

    /** Valor das peças a partir do total gravado e da mão de obra (null se a mão de obra não foi separada). */
    fun parts(total: Long?, labor: Long?): Long? =
        if (total == null || labor == null) null else (total - labor).coerceAtLeast(0L)
}
