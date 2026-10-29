package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT01: ciclos de manutenção e prioridade do alerta (RF-06, RF-11, RN-01, RN-04). */
class CiclosManutencaoTest {

    private val now = System.currentTimeMillis()
    private fun monthsAgo(n: Int): Long = java.util.Calendar.getInstance().apply { timeInMillis = now; add(java.util.Calendar.MONTH, -n) }.timeInMillis
    private val km80 = MaintenanceCycles.evaluate(58_000, listOf(CycleInput("oil", 10_000, null, 50_000, now)), now).single()
    private val tempo = MaintenanceCycles.evaluate(1_000, listOf(CycleInput("brake", null, 6, 1_000, monthsAgo(7))), now).single()

    @Test
    fun `ciclo por km com 8000 de 10000 km rodados fica 80 por cento usado`() {
        assertTrue(km80.usedFraction == 0.8f && km80.kmLeft == 2_000L)
    }

    @Test
    fun `ciclo com 80 por cento conta como proximo e nao como vencido`() {
        assertTrue(km80.soon && !km80.overdue)
    }

    @Test
    fun `ciclo por km tem gatilho KM`() {
        assertEquals(CycleStatus.Trigger.KM, km80.trigger)
    }

    @Test
    fun `ciclo de 6 meses iniciado ha 7 meses esta vencido`() {
        assertTrue(tempo.overdue && tempo.trigger == CycleStatus.Trigger.TIME)
    }

    @Test
    fun `ciclo vencido tem dias restantes negativos`() {
        assertTrue((tempo.daysLeft ?: 0) < 0)
    }

    @Test
    fun `RN-01 vale o criterio que vence primeiro`() {
        val both = MaintenanceCycles.evaluate(53_000, listOf(CycleInput("review", 10_000, 6, 50_000, monthsAgo(5))), now).single()
        assertTrue(both.trigger == CycleStatus.Trigger.TIME && both.usedFraction > 0.8f)
    }

    @Test
    fun `RN-04 ciclo sem intervalo definido nao e monitorado`() {
        val none = MaintenanceCycles.evaluate(60_000, listOf(CycleInput("oil", null, null, 50_000, now), CycleInput("tire", 0, 0, 50_000, now)), now)
        assertTrue(none.isEmpty())
    }

    @Test
    fun `vale o ciclo do servico mais recente do item`() {
        val two = MaintenanceCycles.evaluate(60_000, listOf(
            CycleInput("oil", 10_000, null, 40_000, monthsAgo(12)),
            CycleInput("oil", 10_000, null, 55_000, monthsAgo(2))), now)
        assertTrue(two.size == 1 && two[0].startKm == 55_000L)
    }

    @Test
    fun `lista ordenada do mais urgente para o menos urgente`() {
        val order = MaintenanceCycles.evaluate(60_000, listOf(
            CycleInput("oil", 10_000, null, 59_000, now), CycleInput("brake", 10_000, null, 51_000, now)), now)
        assertEquals("brake", order.first().itemKey)
    }
}
