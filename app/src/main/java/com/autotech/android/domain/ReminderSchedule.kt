package com.autotech.android.domain

import java.util.Calendar
import kotlin.math.roundToLong

/**
 * RN-03: avisos escalonados antes do vencimento de cada ciclo de manutenção.
 *
 * Tempo — referência de um ciclo de 12 meses: avisos a 30, 15 e 7 dias do vencimento.
 *   Ciclos menores recebem prazos proporcionais (6 meses: 15, 8 e 4 dias), nunca menos de 1 dia.
 * Quilometragem — avisos quando faltam 50%, 10% e 5% do intervalo
 *   (10.000 km: quando faltarem 5.000, 1.000 e 500 km).
 * Vencido — um último aviso quando o prazo ou a quilometragem terminam.
 *
 * Cada etapa é avisada uma única vez por ciclo; um novo serviço registrado inicia um ciclo novo.
 */
object ReminderSchedule {

    val BASE_DAYS = listOf(30L, 15L, 7L)
    const val BASE_INTERVAL_DAYS = 365.0
    val KM_FRACTIONS = listOf(0.50, 0.10, 0.05)

    /** Etapa "vencido". */
    const val OVERDUE = 0L

    private const val DAY = 86_400_000L

    enum class Track { TIME, KM }

    data class Reminder(
        val itemKey: String,
        val label: String,
        val track: Track,
        /** Limiar alcançado (dias ou km); [OVERDUE] quando venceu. */
        val stage: Long,
        /** Quanto falta de fato agora (dias ou km); zero ou negativo = vencido. */
        val left: Long,
        val dueDateMillis: Long?,
        val dueKm: Long?,
        /** Identifica o ciclo: muda quando um novo serviço é registrado. */
        val cycleKey: String
    ) {
        val overdue: Boolean get() = stage == OVERDUE
    }

    fun dayThresholds(intervalDays: Long): List<Long> =
        BASE_DAYS.map { maxOf(1L, (it * intervalDays / BASE_INTERVAL_DAYS).roundToLong()) }
            .filter { it < intervalDays }
            .distinct()

    fun kmThresholds(intervalKm: Long): List<Long> =
        KM_FRACTIONS.map { (intervalKm * it).roundToLong() }
            .filter { it in 1 until intervalKm }
            .distinct()

    /** Etapa em que o ciclo está: o menor limiar já alcançado; null se ainda não chegou ao primeiro. */
    fun stageFor(left: Long, thresholds: List<Long>): Long? =
        if (left <= 0) OVERDUE else thresholds.filter { left <= it }.minOrNull()

    /** Só avisa quando a etapa atual é mais avançada do que a última avisada (nunca repete). */
    fun shouldNotify(stage: Long?, lastNotified: Long?): Boolean =
        stage != null && (lastNotified == null || stage < lastNotified)

    /**
     * Avisos devidos agora para um veículo (sem considerar os já enviados).
     * RN-04: só ciclos com intervalo e serviço anterior; vale o serviço mais recente de cada item.
     * Se um dos critérios já venceu, o outro deixa de gerar avisos antecipados para o mesmo ciclo.
     * [skipItems]: itens marcados como feitos ("Já fiz") e ainda pendentes de dados não geram avisos.
     */
    fun due(
        currentKm: Long,
        cycles: List<CycleInput>,
        now: Long = System.currentTimeMillis(),
        skipItems: Set<String> = emptySet()
    ): List<Reminder> =
        cycles
            .filter { (it.intervalKm ?: 0) > 0 || (it.intervalMonths ?: 0) > 0 }
            .filter { it.itemKey !in skipItems }
            .groupBy { it.itemKey }
            .mapNotNull { (_, list) -> list.maxByOrNull { it.serviceDateMillis } }
            .flatMap { c ->
                val key = "${c.itemKey}:${c.serviceDateMillis}:${c.serviceKm}"
                val label = MaintenanceCatalog.label(c.itemKey)
                val list = mutableListOf<Reminder>()
                c.intervalMonths?.takeIf { it > 0 }?.let { months ->
                    val due = Calendar.getInstance().apply {
                        timeInMillis = c.serviceDateMillis
                        add(Calendar.MONTH, months)
                    }.timeInMillis
                    val intervalDays = Math.floorDiv(due - c.serviceDateMillis, DAY)
                    val left = Math.floorDiv(due - now, DAY)
                    stageFor(left, dayThresholds(intervalDays))?.let {
                        list += Reminder(c.itemKey, label, Track.TIME, it, left, due, null, key)
                    }
                }
                c.intervalKm?.takeIf { it > 0 }?.let { interval ->
                    val dueKm = c.serviceKm + interval
                    val left = dueKm - currentKm
                    stageFor(left, kmThresholds(interval.toLong()))?.let {
                        list += Reminder(c.itemKey, label, Track.KM, it, left, null, dueKm, key)
                    }
                }
                if (list.any { it.overdue }) list.filter { it.overdue } else list
            }
}
