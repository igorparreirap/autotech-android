package com.autotech.android.domain

/**
 * RF-05: consumo (km/L) pelo método "tanque a tanque".
 *
 * Quando o usuário marca "calcular consumo" num abastecimento, o sistema pega os km rodados desde
 * o abastecimento anterior e divide pelos litros que entraram agora (o que foi gasto no período).
 * O combustível queimado no período é o que foi colocado no abastecimento ANTERIOR — por isso, em
 * veículo flex, o rendimento por combustível usa a composição informada no início do período.
 */
data class FillUp(
    val id: Long,
    val dateMillis: Long,
    val km: Long,
    val liters: Double,
    /** Só em veículo flex, quando o usuário especifica (opcional). */
    val ethanolLiters: Double? = null,
    val gasolineLiters: Double? = null,
    val computeConsumption: Boolean = false
)

enum class FuelMix { ETHANOL, GASOLINE, MIXED, UNKNOWN }

data class ConsumptionPeriod(
    val fillId: Long,
    val fromKm: Long,
    val toKm: Long,
    val fromDateMillis: Long,
    val toDateMillis: Long,
    val liters: Double,
    /** Combustível que estava no tanque (o do abastecimento que abriu o período). */
    val burned: FuelMix
) {
    val km: Long get() = toKm - fromKm
    val kmPerLiter: Double get() = km / liters
}

data class ConsumptionSummary(
    /** Média de todos os períodos calculados (soma dos km / soma dos litros). */
    val overall: Double?,
    val ethanol: Double?,
    val gasoline: Double?,
    val periods: Int
) {
    /** Quanto o álcool rende em relação à gasolina, com os dados do próprio veículo (ex.: 0,72). */
    val ethanolVsGasoline: Double? get() = if (ethanol != null && gasoline != null && gasoline > 0) ethanol / gasoline else null
}

object FuelConsumption {

    fun mixOf(ethanolLiters: Double?, gasolineLiters: Double?): FuelMix {
        val e = ethanolLiters ?: 0.0
        val g = gasolineLiters ?: 0.0
        return when {
            e > 0 && g > 0 -> FuelMix.MIXED
            e > 0 -> FuelMix.ETHANOL
            g > 0 -> FuelMix.GASOLINE
            else -> FuelMix.UNKNOWN
        }
    }

    /** Abastecimento imediatamente anterior pela quilometragem. */
    fun previousOf(fills: List<FillUp>, km: Long, excludeId: Long? = null): FillUp? =
        fills.filter { it.km < km && it.id != excludeId }.maxWithOrNull(compareBy({ it.km }, { it.dateMillis }))

    /** Período de consumo terminado neste abastecimento; null se não pediu o cálculo ou não há base. */
    fun period(fill: FillUp, previous: FillUp?): ConsumptionPeriod? {
        if (!fill.computeConsumption || previous == null) return null
        if (fill.km <= previous.km || fill.liters <= 0.0) return null
        return ConsumptionPeriod(
            fillId = fill.id,
            fromKm = previous.km,
            toKm = fill.km,
            fromDateMillis = previous.dateMillis,
            toDateMillis = fill.dateMillis,
            liters = fill.liters,
            burned = mixOf(previous.ethanolLiters, previous.gasolineLiters)
        )
    }

    /** Todos os períodos calculados do veículo, do mais recente para o mais antigo. */
    fun periods(fills: List<FillUp>): List<ConsumptionPeriod> =
        fills.mapNotNull { f -> period(f, previousOf(fills, f.km, f.id)) }
            .sortedByDescending { it.toKm }

    fun summarize(periods: List<ConsumptionPeriod>): ConsumptionSummary {
        fun avg(list: List<ConsumptionPeriod>): Double? {
            val liters = list.sumOf { it.liters }
            return if (list.isEmpty() || liters <= 0) null else list.sumOf { it.km } / liters
        }
        return ConsumptionSummary(
            overall = avg(periods),
            ethanol = avg(periods.filter { it.burned == FuelMix.ETHANOL }),
            gasoline = avg(periods.filter { it.burned == FuelMix.GASOLINE }),
            periods = periods.size
        )
    }
}
