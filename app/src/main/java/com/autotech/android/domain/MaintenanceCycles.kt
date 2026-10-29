package com.autotech.android.domain

import java.util.Calendar

/**
 * Ciclo configurado pelo usuário (RF-11) junto do serviço que serve de marco zero (RN-04).
 * Modelo do domínio: não depende do banco de dados nem da interface.
 */
data class CycleInput(
    val itemKey: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val serviceKm: Long,
    val serviceDateMillis: Long
)

/** Situação de um ciclo de manutenção em relação ao odômetro e à data atuais. */
data class CycleStatus(
    val itemKey: String,
    val label: String,
    /** 0 = acabou de fazer, 1 = venceu agora, > 1 = vencido. */
    val usedFraction: Float,
    val kmLeft: Long?,
    val daysLeft: Long?,
    val startKm: Long,
    val endKm: Long?,
    /** Critério que está mais perto de vencer (RN-01). */
    val trigger: Trigger
) {
    enum class Trigger { KM, TIME }

    val overdue: Boolean get() = usedFraction >= 1f

    /** Aviso antecipado: 80% ou mais do intervalo já consumido (RF-06). */
    val soon: Boolean get() = usedFraction >= SOON_FRACTION

    companion object {
        const val SOON_FRACTION = 0.8f
    }
}

/** Motor de cálculo dos ciclos (RF-06, RF-11, RN-01, RN-04). */
object MaintenanceCycles {

    /**
     * - RN-04: só entram ciclos com intervalo definido (km e/ou meses) e serviço anterior;
     * - RN-01: tempo e quilometragem são avaliados em paralelo e vale o que estiver mais perto de vencer;
     * - para cada item, vale o ciclo do serviço mais recente.
     * Resultado ordenado do mais urgente para o menos urgente.
     */
    fun evaluate(currentKm: Long, cycles: List<CycleInput>, now: Long = System.currentTimeMillis()): List<CycleStatus> =
        cycles
            .filter { (it.intervalKm ?: 0) > 0 || (it.intervalMonths ?: 0) > 0 }
            .groupBy { it.itemKey }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.serviceDateMillis } }
            .map { c -> evaluateOne(currentKm, c, now) }
            .sortedByDescending { it.usedFraction }

    private fun evaluateOne(currentKm: Long, c: CycleInput, now: Long): CycleStatus {
        var kmFrac: Float? = null
        var kmLeft: Long? = null
        var endKm: Long? = null
        c.intervalKm?.takeIf { it > 0 }?.let { interval ->
            val used = currentKm - c.serviceKm
            kmFrac = used.toFloat() / interval
            kmLeft = interval - used
            endKm = c.serviceKm + interval
        }
        var timeFrac: Float? = null
        var daysLeft: Long? = null
        c.intervalMonths?.takeIf { it > 0 }?.let { months ->
            val due = Calendar.getInstance().apply {
                timeInMillis = c.serviceDateMillis
                add(Calendar.MONTH, months)
            }.timeInMillis
            val total = (due - c.serviceDateMillis).coerceAtLeast(1L)
            timeFrac = (now - c.serviceDateMillis).toFloat() / total
            daysLeft = Math.floorDiv(due - now, 86_400_000L)
        }
        val k = kmFrac ?: Float.NEGATIVE_INFINITY
        val t = timeFrac ?: Float.NEGATIVE_INFINITY
        return CycleStatus(
            itemKey = c.itemKey,
            label = MaintenanceCatalog.label(c.itemKey),
            usedFraction = maxOf(k, t).coerceAtLeast(0f),
            kmLeft = kmLeft,
            daysLeft = daysLeft,
            startKm = c.serviceKm,
            endKm = endKm,
            trigger = if (k >= t) CycleStatus.Trigger.KM else CycleStatus.Trigger.TIME
        )
    }
}
