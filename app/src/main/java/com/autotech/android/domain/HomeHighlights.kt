package com.autotech.android.domain

/**
 * RF-06: o que a tela inicial destaca de cada veículo. Fica no domínio para ser testado sem a interface.
 */
object HomeHighlights {

    /**
     * Odômetro: a troca por quilometragem mais próxima, isto é, a de menor km restante
     * (vencidas, com km restante negativo, vêm antes). Não usa a fração consumida, porque um
     * intervalo longo pode ter fração maior e ainda estar muito mais longe.
     */
    fun nearestByKm(statuses: List<CycleStatus>): CycleStatus? =
        statuses.filter { it.kmLeft != null }.minByOrNull { it.kmLeft!! }

    /** Medidor "Revisão": só o ciclo da revisão; os demais itens ficam em "Próximas trocas". */
    fun review(statuses: List<CycleStatus>): CycleStatus? =
        statuses.filter { it.itemKey in MaintenanceCatalog.REVIEW_KEYS }.maxByOrNull { it.usedFraction }

    /**
     * Lembretes: só os itens para os quais o motor de notificações (RN-03) já emitiria um aviso agora.
     * Fora das etapas de aviso, a seção não aparece na tela inicial.
     */
    fun reminderKeys(
        currentKm: Long,
        cycles: List<CycleInput>,
        now: Long = System.currentTimeMillis(),
        skipItems: Set<String> = emptySet()
    ): Set<String> =
        ReminderSchedule.due(currentKm, cycles, now, skipItems).map { it.itemKey }.toSet()
}

/** RF-11: prazo do alerta informado em meses ou em anos; o ciclo é sempre gravado em meses. */
object IntervalUnits {

    fun toMonths(value: Int?, inYears: Boolean): Int? = value?.let { if (inYears) it * 12 else it }

    /** Para mostrar um prazo gravado: múltiplos de 12 meses aparecem em anos. */
    fun fromMonths(months: Int?): Pair<Int?, Boolean> =
        if (months != null && months >= 12 && months % 12 == 0) (months / 12) to true else months to false
}
