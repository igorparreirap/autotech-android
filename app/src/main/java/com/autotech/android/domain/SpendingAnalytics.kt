package com.autotech.android.domain

import java.util.Calendar

/** Lançamento (manutenção ou abastecimento) já normalizado para as análises do painel (RF-08). */
data class Expense(
    val vehicleId: Long,
    /** data do serviço ou do abastecimento (nunca a data do registro) */
    val date: Long,
    val km: Long,
    val amount: Double,
    val category: String,
    val isFuel: Boolean,
    val liters: Double,
    val itemKey: String?
)

enum class SpendPeriod { CURRENT_MONTH, LAST_SIX_MONTHS, CURRENT_YEAR, ALL, CUSTOM }

enum class SpendGrouping { MONTH, YEAR, KM }

/**
 * Agrupamento de gastos. Em MONTH e YEAR, [start] e [end] são datas (milissegundos);
 * em KM, são a quilometragem inicial (inclusiva) e final (exclusiva) da faixa.
 */
data class SpendBucket(val start: Long, val end: Long, val maintenance: Double, val fuel: Double) {
    val total: Double get() = maintenance + fuel
}

/** Cálculos do painel financeiro (RF-08). Funções puras, sem dependência de interface ou banco. */
object SpendingAnalytics {

    private const val DAY = 86_400_000L

    fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun dayEnd(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = dayStart(millis)
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis - 1

    fun monthStart(millis: Long, minusMonths: Int = 0): Long = Calendar.getInstance().apply {
        timeInMillis = dayStart(millis)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, -minusMonths)
    }.timeInMillis

    fun yearOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)

    /** Intervalo de datas de cada período do filtro. */
    fun range(period: SpendPeriod, all: List<Expense>, customStart: Long, customEnd: Long, now: Long = System.currentTimeMillis()): Pair<Long, Long> =
        when (period) {
            SpendPeriod.CURRENT_MONTH -> monthStart(now) to dayEnd(now)
            SpendPeriod.LAST_SIX_MONTHS -> monthStart(now, 5) to dayEnd(now)
            SpendPeriod.CURRENT_YEAR -> Calendar.getInstance().apply {
                timeInMillis = monthStart(now); set(Calendar.MONTH, Calendar.JANUARY)
            }.timeInMillis to dayEnd(now)
            SpendPeriod.ALL -> monthStart(all.minOfOrNull { it.date } ?: now) to dayEnd(maxOf(now, all.maxOfOrNull { it.date } ?: now))
            SpendPeriod.CUSTOM -> dayStart(minOf(customStart, customEnd)) to dayEnd(maxOf(customStart, customEnd))
        }

    /** Período anterior de mesma duração, usado na variação dos indicadores. */
    fun previousRange(start: Long, end: Long): Pair<Long, Long> = (start - (end - start) - 1) to (start - 1)

    /** Variação percentual; null quando não há base de comparação. */
    fun variation(current: Double, previous: Double): Double? =
        if (previous > 0) (current - previous) / previous * 100 else null

    /** Km rodados: soma, por veículo, da diferença entre a maior e a menor quilometragem. */
    fun kmDriven(list: List<Expense>): Long =
        list.groupBy { it.vehicleId }.values.sumOf { e -> if (e.size < 2) 0L else e.maxOf { it.km } - e.minOf { it.km } }

    // Consumo (km/L) não faz parte do painel financeiro: é calculado só a pedido do usuário,
    // entre abastecimentos seguidos (domain/FuelConsumption, RF-05).

    /** Preço médio do litro, considerando só abastecimentos com valor e litros informados. */
    fun averageLiterPrice(list: List<Expense>): Double? {
        val paid = list.filter { it.isFuel && it.amount > 0 && it.liters > 0 }
        return if (paid.isEmpty()) null else paid.sumOf { it.amount } / paid.sumOf { it.liters }
    }

    /** Total por categoria, do maior para o menor, sem categorias zeradas. */
    fun byCategory(list: List<Expense>): List<Pair<String, Double>> =
        list.groupBy { it.category }
            .map { (k, v) -> k to v.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    private fun bucketOf(start: Long, end: Long, items: List<Expense>) = SpendBucket(
        start, end,
        items.filter { !it.isFuel }.sumOf { it.amount },
        items.filter { it.isFuel }.sumOf { it.amount }
    )

    /** Agrupa os gastos por mês, por ano ou por faixa de quilometragem. */
    fun buckets(list: List<Expense>, grouping: SpendGrouping, start: Long, end: Long): List<SpendBucket> =
        when (grouping) {
            SpendGrouping.MONTH -> {
                val cal = Calendar.getInstance().apply { timeInMillis = monthStart(start) }
                val out = mutableListOf<SpendBucket>()
                while (cal.timeInMillis <= end && out.size < 120) {
                    val s = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val e = cal.timeInMillis - 1
                    out += bucketOf(s, e, list.filter { it.date in s..e })
                }
                out
            }
            SpendGrouping.YEAR -> (yearOf(start)..yearOf(end)).map { y ->
                val s = Calendar.getInstance().apply { clear(); set(y, Calendar.JANUARY, 1) }.timeInMillis
                val e = Calendar.getInstance().apply { clear(); set(y + 1, Calendar.JANUARY, 1) }.timeInMillis - 1
                bucketOf(s, e, list.filter { yearOf(it.date) == y })
            }
            SpendGrouping.KM -> {
                if (list.isEmpty()) {
                    emptyList()
                } else {
                    val minKm = list.minOf { it.km }
                    val maxKm = list.maxOf { it.km }
                    val size = kmStep(maxKm - minKm)
                    val out = mutableListOf<SpendBucket>()
                    var b = (minKm / size) * size
                    while (b <= maxKm) {
                        val from = b
                        out += bucketOf(from, from + size, list.filter { it.km >= from && it.km < from + size })
                        b += size
                    }
                    out
                }
            }
        }

    /** Tamanho da faixa de km para gerar no máximo cerca de 9 barras. */
    fun kmStep(span: Long): Long {
        val steps = listOf(500L, 1_000L, 2_000L, 2_500L, 5_000L, 10_000L, 20_000L, 25_000L, 50_000L, 100_000L)
        return steps.firstOrNull { span / it <= 8 } ?: 100_000L
    }
}
