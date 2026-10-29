package com.autotech.android.domain

import kotlin.math.abs

/** RN-06: comparação de custo entre álcool e gasolina (RF-13). */
object FuelComparison {

    /** O álcool compensa se custar até 70% do preço da gasolina. */
    const val ETHANOL_LIMIT = 0.70

    enum class Verdict { ETHANOL, GASOLINE, EQUIVALENT }

    data class Result(
        val verdict: Verdict,
        /** preço do álcool ÷ preço da gasolina */
        val ratio: Double,
        /** maior preço do álcool que ainda compensa para a gasolina informada */
        val maxEthanolPrice: Double
    )

    /** Retorna null enquanto algum dos preços não for válido (maior que zero). */
    fun compare(gasolinePrice: Double?, ethanolPrice: Double?): Result? {
        val g = gasolinePrice?.takeIf { it > 0 } ?: return null
        val e = ethanolPrice?.takeIf { it > 0 } ?: return null
        val ratio = e / g
        val verdict = when {
            abs(ratio - ETHANOL_LIMIT) < 0.0005 -> Verdict.EQUIVALENT
            ratio < ETHANOL_LIMIT -> Verdict.ETHANOL
            else -> Verdict.GASOLINE
        }
        return Result(verdict, ratio, g * ETHANOL_LIMIT)
    }
}
