package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT08: indicadores do painel financeiro e custo da manutenção (RF-04, RF-08). Valores em centavos. */
class PainelFinanceiroTest {

    private val now = System.currentTimeMillis()
    private fun monthsAgo(n: Int): Long = java.util.Calendar.getInstance().apply { timeInMillis = now; add(java.util.Calendar.MONTH, -n) }.timeInMillis
    private val dia = monthsAgo(1)
    private val lista = listOf(
        Expense(1, dia, 1_000, 20000L, "Combustível", true, 40.0, null),
        Expense(1, dia, 1_400, 15000L, "Combustível", true, 30.0, null),
        Expense(1, dia, 1_800, 17500L, "Combustível", true, 35.0, null),
        Expense(1, dia, 1_500, 30000L, "Óleo / filtro", false, 0.0, "oil"),
        Expense(2, dia, 90_000, 5000L, "Combustível", true, 10.0, null),
        Expense(2, dia, 90_500, 0L, "Pneus", false, 0.0, "tire"))
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
        assertEquals(20.0, SpendingAnalytics.variation(12_000L, 10_000L)!!, 0.01)
    }

    @Test
    fun `sem periodo anterior nao ha variacao`() {
        assertNull(SpendingAnalytics.variation(5_000L, 0L))
    }

    @Test
    fun `filtro de 6 meses gera 6 barras mensais`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.LAST_SIX_MONTHS, lista, 0, 0, now)
        assertEquals(6, SpendingAnalytics.buckets(lista, SpendGrouping.MONTH, s, e).size)
    }

    @Test
    fun `barras mensais somam o total do periodo`() {
        val (s, e) = SpendingAnalytics.range(SpendPeriod.LAST_SIX_MONTHS, lista, 0, 0, now)
        assertEquals(lista.sumOf { it.amountCents }, SpendingAnalytics.buckets(lista, SpendGrouping.MONTH, s, e).sumOf { it.total })
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
        assertEquals(veiculo1.sumOf { it.amountCents }, SpendingAnalytics.buckets(veiculo1, SpendGrouping.KM, 0, now).sumOf { it.total })
    }

    @Test
    fun `12000 km rodados geram faixas de 2000 km`() {
        assertEquals(2_000L, SpendingAnalytics.kmStep(12_000))
    }

    @Test
    fun `pecas de 350 com mao de obra de 120 somam 470`() {
        assertEquals(47_000L, MaintenanceCost.total(35_000L, 12_000L))
    }

    @Test
    fun `so mao de obra informada vira o total`() {
        assertEquals(12_000L, MaintenanceCost.total(null, 12_000L))
    }

    @Test
    fun `so pecas informadas viram o total`() {
        assertEquals(35_000L, MaintenanceCost.total(35_000L, null))
    }

    @Test
    fun `nada informado nao tem total`() {
        assertNull(MaintenanceCost.total(null, null))
    }

    @Test
    fun `pecas sao recuperadas do total menos a mao de obra`() {
        assertEquals(35_000L, MaintenanceCost.parts(47_000L, 12_000L))
    }

    @Test
    fun `sem mao de obra separada as pecas nao sao divididas`() {
        assertNull(MaintenanceCost.parts(47_000L, null))
    }

    // RF-04: manutenção com várias peças, cada uma com o próprio valor e observação
    private val pecas = listOf(
        ItemEntry("custom:suspensao:Bucha da bandeja", 8_000L, "Marca Nakata"),
        ItemEntry("custom:suspensao:Pivo da bandeja", 12_000L, "Comprado na Auto Pecas Centro"),
        ItemEntry("shock_front", 45_000L, null))

    @Test
    fun `cada peca guarda o proprio valor e a mao de obra fica no principal`() {
        val r = MaintenanceSplit.records(pecas, 15_000L, "Barulho na frente resolvido", "Feito no mesmo atendimento")
        assertEquals(3, r.size)
        assertEquals(23_000L, r[0].amount)
        assertEquals(15_000L, r[0].labor)
        assertEquals(12_000L, r[1].amount)
        assertNull(r[1].labor)
        assertEquals(45_000L, r[2].amount)
    }

    @Test
    fun `soma dos registros e igual ao total pago`() {
        val r = MaintenanceSplit.records(pecas, 15_000L, null, null)
        assertEquals(80_000L, r.sumOf { it.amount ?: 0L })
        assertEquals(80_000L, MaintenanceSplit.total(pecas, 15_000L))
    }

    @Test
    fun `observacao do item e descricao geral ficam em linhas separadas`() {
        val r = MaintenanceSplit.records(pecas, null, "Barulho na frente resolvido", "Feito no mesmo atendimento")
        assertEquals("Marca Nakata\nBarulho na frente resolvido", r[0].description)
        assertEquals("Comprado na Auto Pecas Centro\nFeito no mesmo atendimento", r[1].description)
        assertEquals("Feito no mesmo atendimento", r[2].description)
    }

    @Test
    fun `item sem valor nem mao de obra fica sem valor`() {
        val r = MaintenanceSplit.records(listOf(ItemEntry("oil", null, null), ItemEntry("oil_filter", null, "  ")), null, "  ", null)
        assertNull(r[0].amount)
        assertNull(r[0].description)
        assertNull(r[1].description)
        assertNull(MaintenanceSplit.total(listOf(ItemEntry("oil", null, null)), null))
    }

    @Test
    fun `item repetido entra uma vez so`() {
        val r = MaintenanceSplit.records(listOf(ItemEntry("oil", 5_000L, null), ItemEntry("oil", 5_000L, null)), null, null, null)
        assertEquals(1, r.size)
    }

    @Test
    fun `revisao guarda o proprio valor e o de cada item trocado`() {
        val itens = listOf(
            ItemEntry("review", 45_000L, null),
            ItemEntry("oil", 22_000L, "Oleo 5W30 sintetico"),
            ItemEntry("air_filter", 6_000L, null),
            ItemEntry("brake_fluid", null, "So verificado"))
        val r = MaintenanceSplit.records(itens, 31_500L, "Revisao dos 40 mil km", "Feito na revisao periodica")
        assertEquals(76_500L, r[0].amount)
        assertEquals(22_000L, r[1].amount)
        assertNull(r[3].amount)
        assertEquals("So verificado\nFeito na revisao periodica", r[3].description)
        assertEquals(104_500L, r.sumOf { it.amount ?: 0L })
    }
}
