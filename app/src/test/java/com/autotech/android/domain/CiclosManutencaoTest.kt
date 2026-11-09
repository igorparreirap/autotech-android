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

    @Test
    fun `troca mais nova sem intervalo encerra o ciclo anterior do item`() {
        val antigo = CycleInput("wheel", 10_000, 12, 40_000, monthsAgo(14))
        val restantes = MaintenanceCycles.dropSuperseded(listOf(antigo), mapOf("wheel" to now))
        assertTrue(restantes.isEmpty())
    }

    @Test
    fun `ciclo do servico mais recente continua monitorado`() {
        val atual = CycleInput("oil", 10_000, null, 50_000, now)
        val restantes = MaintenanceCycles.dropSuperseded(listOf(atual), mapOf("oil" to now))
        assertEquals(listOf(atual), restantes)
    }

    // Tela inicial: 183.000 km, óleo a cada 10.000 km (feito aos 180.000) e correia a cada 60.000 km (feita aos 162.000)
    private val oleoECorreia = MaintenanceCycles.evaluate(183_000, listOf(
        CycleInput("oil", 10_000, null, 180_000, now), CycleInput("timing", 60_000, null, 162_000, now)), now)

    @Test
    fun `odometro aponta a troca de oleo que falta menos km`() {
        val alvo = HomeHighlights.nearestByKm(oleoECorreia)
        assertEquals("oil", alvo?.itemKey)
        assertEquals(7_000L, alvo?.kmLeft)
    }

    @Test
    fun `fracao consumida maior nao decide o odometro`() {
        assertEquals("timing", oleoECorreia.first().itemKey)
        assertEquals("oil", HomeHighlights.nearestByKm(oleoECorreia)?.itemKey)
    }

    @Test
    fun `medidor de revisao ignora os outros itens`() {
        val lista = MaintenanceCycles.evaluate(10_000, listOf(
            CycleInput("oil", 1_000, null, 9_500, now), CycleInput("review", 10_000, null, 8_000, now)), now)
        assertEquals("review", HomeHighlights.review(lista)?.itemKey)
    }

    @Test
    fun `sem revisao programada o medidor fica vazio`() {
        assertNull(HomeHighlights.review(oleoECorreia))
    }

    @Test
    fun `prazo de 4 anos vira 48 meses`() {
        assertEquals(48, IntervalUnits.toMonths(4, inYears = true))
        assertEquals(18, IntervalUnits.toMonths(18, inYears = false))
        assertNull(IntervalUnits.toMonths(null, inYears = true))
    }

    @Test
    fun `prazo gravado em multiplo de 12 meses aparece em anos`() {
        assertEquals(4 to true, IntervalUnits.fromMonths(48))
        assertEquals(18 to false, IntervalUnits.fromMonths(18))
    }
}
