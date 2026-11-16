package com.autotech.android.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * RF-04 / RF-05 / RF-08: valores em dinheiro guardados em centavos (Long).
 *
 * Somas e diferenças de centavos são exatas; com Double, valores como 0,10 + 0,20 não têm
 * representação binária exata e os totais do painel e do relatório poderiam divergir por
 * arredondamento. A conversão para reais só acontece na hora de mostrar o valor.
 */
object Money {

    /**
     * Lê o que o usuário digitou ("1.275,90", "289,9", "289.90", "R$ 80") e devolve centavos.
     * Vírgula é a casa decimal; ponto com três dígitos depois é separador de milhar.
     * Devolve null para campo vazio, texto inválido ou valor negativo.
     */
    fun parseCents(raw: String?): Long? {
        var s = raw?.trim()?.removePrefix("R$")?.trim()?.replace(" ", "") ?: return null
        if (s.isEmpty()) return null
        s = when {
            s.contains(',') -> s.replace(".", "").replace(',', '.')
            s.count { it == '.' } == 1 && s.substringAfter('.').length == 3 -> s.replace(".", "")
            s.count { it == '.' } > 1 -> s.replace(".", "")
            else -> s
        }
        val value = s.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }

    /** Centavos em reais, sem perda (para formatar com NumberFormat). */
    fun toReais(cents: Long): BigDecimal = BigDecimal.valueOf(cents, 2)

    /** Reais (Double) em centavos, arredondando para o centavo mais próximo. */
    fun fromReais(reais: Double): Long = BigDecimal.valueOf(reais).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
}
