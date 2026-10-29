package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT08: indicadores do painel financeiro e custo da manutenção (RF-04, RF-08). */
class PainelFinanceiroTest {

    private val now = System.currentTimeMillis()
    private fun monthsAgo(n: Int): Long = java.util.Calendar.getInstance().apply { timeInMillis = now; add(java.util.Calendar.MONTH, -n) }.timeInMillis
    private val dia = monthsAgo(1)
    private val lista = listOf(
        Expense(1, dia, 1_000, 200.0, "Combustível", true, 40.0, null),
        Expense(1, dia, 1_400, 150.0, "Combustível", true, 30.0, null),
        Expense(1, dia, 1_800, 175.0, "Combustível", true, 35.0, null),
        Expense(1, dia, 1_500, 300.0, "Óleo / filtro", false, 0.0, "oil"),
        Expense(2, dia, 90_000, 50.0, "Combustível", true, 10.0, null),
        Expense(2, dia, 90_500, 0.0, "Pneus", false, 0.0, "tire"))
    private val veiculo1 = lista.filter { it.vehicleId == 1L }

    @Test
    fun `km rodados somam 800 e 500 km`() {
        assertEquals(1_300L, SpendingAnalytics.kmDriven(lista))
    }

    @Test
    fun `preco medio do litro e 575 reais por 115 litros`() {
        assertEquals(575.0 / 115.0, SpendingAnalytics.averageLiterPrice(lista)!!, 0.01)
    }

    @Test
    fun `categorias com maior gasto primeiro e sem categoria zerada`() {
        val cats = SpendingAnalytics.byCategory(lista)
        assertTrue(cats.first().first == "Combustível" && cats.none { it.first == "Pneus" })
    }

    @Test
    fun `variacao de 120 contra 100 e de 20 por cento`() {
        assertEquals(20.0, SpendingAnalytics.variation(120.0, 100.0)!!, 0.01)
    }

    @Test
    fun `sem periodo anterior nao ha variacao`() {
        assertNull(SpendingAnalytics.variation(50.0, 0.0))
    }

    @Test
    fun `filtro de 6 meses gera 6 barras mensais`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.LAST_SIX_MONTHS, lista, 0, 0, now)
        assertEquals(6, SpendingAnalytics.buckets(lista, SpendGrouping.MONTH, s, e).size)
    }

    @Test
    fun `barras mensais somam o total do periodo`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.LAST_SIX_MONTHS, lista, 0, 0, now)
        assertEquals(lista.sumOf { it.amount }, SpendingAnalytics.buckets(lista, SpendGrouping.MONTH, s, e).sumOf { it.total }, 0.01)
    }

    @Test
    fun `filtro Este mes gera 1 barra`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.CURRENT_MONTH, lista, 0, 0, now)
        assertEquals(1, SpendingAnalytics.buckets(lista, SpendGrouping.MONTH, s, e).size)
    }

    @Test
    fun `periodo anterior termina antes do atual comecar`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.CURRENT_MONTH, lista, 0, 0, now)
        val (ps, pe) = SpendingAnalytics.previousRange(s, e)
        assertTrue(pe < s && (pe - ps) == (e - s))
    }

    @Test
    fun `periodo personalizado aceita datas invertidas`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.CUSTOM, lista, now, monthsAgo(2), now)
        assertTrue(s < e)
    }

    @Test
    fun `faixas de km comecam em 1000 km com passo de 500`() {
        val kmB = SpendingAnalytics.buckets(veiculo1, SpendGrouping.KM, 0, now)
        assertTrue(SpendingAnalytics.kmStep(800) == 500L && kmB.first().start == 1_000L)
    }

    @Test
    fun `faixas de km somam o total do veiculo`() {
        assertEquals(veiculo1.sumOf { it.amount }, SpendingAnalytics.buckets(veiculo1, SpendGrouping.KM, 0, now).sumOf { it.total }, 0.01)
    }

    @Test
    fun `12000 km rodados geram faixas de 2000 km`() {
        assertEquals(2_000L, SpendingAnalytics.kmStep(12_000))
    }

    @Test
    fun `pecas de 350 com mao de obra de 120 somam 470`() {
        assertEquals(470.0, MaintenanceCost.total(350.0, 120.0)!!, 0.001)
    }

    @Test
    fun `so mao de obra informada vira o total`() {
        assertEquals(120.0, MaintenanceCost.total(null, 120.0)!!, 0.001)
    }

    @Test
    fun `so pecas informadas viram o total`() {
        assertEquals(350.0, MaintenanceCost.total(350.0, null)!!, 0.001)
    }

    @Test
    fun `nada informado nao tem total`() {
        assertNull(MaintenanceCost.total(null, null))
    }

    @Test
    fun `pecas sao recuperadas do total menos a mao de obra`() {
        assertEquals(350.0, MaintenanceCost.parts(470.0, 120.0)!!, 0.001)
    }

    @Test
    fun `sem mao de obra separada as pecas nao sao divididas`() {
        assertNull(MaintenanceCost.parts(470.0, null))
    }
}
