package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT04: abastecimento (RF-05): combustível informado, consumo tanque a tanque e comprovantes. */
class ConsumoCombustivelTest {

    private val d = 1_000L
    private val a = FillUp(1, d, 10_000, 40.0, ethanolLiters = 40.0)
    private val b = FillUp(2, d * 2, 10_450, 50.0, ethanolLiters = 50.0, computeConsumption = true)
    private val c = FillUp(3, d * 3, 10_900, 40.0, gasolineLiters = 40.0, computeConsumption = true)
    private val e = FillUp(4, d * 4, 11_500, 50.0, computeConsumption = true)
    private val f = FillUp(5, d * 5, 11_800, 20.0)
    private val fills = listOf(a, b, c, e, f)
    private val periods = FuelConsumption.periods(fills)
    private val resumo = FuelConsumption.summarize(periods)
    private fun near(x: Double?, y: Double) = x != null && kotlin.math.abs(x - y) < 0.01

    @Test
    fun `primeiro abastecimento nao tem consumo`() {
        assertNull(FuelConsumption.period(a.copy(computeConsumption = true), null))
    }

    @Test
    fun `450 km com 50 litros resultam em 9 km por litro`() {
        val p = FuelConsumption.period(b, a)!!
        assertTrue(near(p.kmPerLiter, 9.0) && p.km == 450L)
    }

    @Test
    fun `combustivel queimado e o do abastecimento anterior`() {
        assertEquals(FuelMix.ETHANOL, FuelConsumption.period(b, a)!!.burned)
    }

    @Test
    fun `sem marcar calcular nao ha calculo`() {
        assertNull(FuelConsumption.period(f, e))
    }

    @Test
    fun `anterior e o de maior km abaixo do atual`() {
        assertEquals(2L, FuelConsumption.previousOf(fills, 10_900, 3)?.id)
    }

    @Test
    fun `km igual ou menor que o anterior nao calcula`() {
        assertNull(FuelConsumption.period(b.copy(km = 10_000), a))
    }

    @Test
    fun `tres periodos calculados`() {
        assertEquals(3, periods.size)
    }

    @Test
    fun `periodo mais recente primeiro`() {
        assertEquals(4L, periods.first().fillId)
    }

    @Test
    fun `media geral de 1500 km com 140 litros`() {
        assertTrue(near(resumo.overall, 1500.0 / 140.0))
    }

    @Test
    fun `media com etanol de 10 km por litro`() {
        assertTrue(near(resumo.ethanol, 10.0))
    }

    @Test
    fun `media com gasolina de 12 km por litro`() {
        assertTrue(near(resumo.gasoline, 12.0))
    }

    @Test
    fun `etanol rende 83 por cento da gasolina neste carro`() {
        assertTrue(near(resumo.ethanolVsGasoline, 10.0 / 12.0))
    }

    @Test
    fun `mistura de etanol e gasolina e MIXED`() {
        assertEquals(FuelMix.MIXED, FuelConsumption.mixOf(20.0, 20.0))
    }

    @Test
    fun `combustivel nao especificado e UNKNOWN`() {
        assertEquals(FuelMix.UNKNOWN, FuelConsumption.mixOf(null, null))
    }

    @Test
    fun `periodo com mistura entra so na media geral`() {
        val m = FuelConsumption.summarize(listOf(FuelConsumption.period(FillUp(9, d, 1_500, 30.0, computeConsumption = true), FillUp(8, 0, 1_000, 30.0, 15.0, 15.0))!!))
        assertTrue(near(m.overall, 500.0 / 30.0) && m.ethanol == null && m.gasoline == null)
    }

    @Test
    fun `carro nao flex tem so media geral`() {
        val s = FuelConsumption.summarize(FuelConsumption.periods(listOf(FillUp(1, d, 0, 40.0), FillUp(2, d, 500, 40.0, computeConsumption = true))))
        assertTrue(near(s.overall, 12.5) && s.ethanolVsGasoline == null)
    }

    @Test
    fun `sem periodos nao ha media`() {
        assertNull(FuelConsumption.summarize(emptyList()).overall)
    }

    // ── Combustível do abastecimento flex e vários comprovantes por registro

    @Test
    fun `flex sem escolher o combustivel e recusado`() {
        val r = FuelEntry.liters(true, null, 40.0, null, null)
        assertTrue(r.isFailure)
        assertEquals("Escolha o combustível: etanol, gasolina ou os dois.", r.exceptionOrNull()?.message)
    }

    @Test
    fun `flex so com etanol grava os litros como etanol`() {
        val l = FuelEntry.liters(true, FuelChoice.ETHANOL, 42.5, null, null).getOrThrow()
        assertEquals(42.5, l.total, 0.001)
        assertEquals(42.5, l.ethanol!!, 0.001)
        assertNull(l.gasoline)
    }

    @Test
    fun `flex so com gasolina grava os litros como gasolina`() {
        val l = FuelEntry.liters(true, FuelChoice.GASOLINE, 30.0, null, null).getOrThrow()
        assertEquals(30.0, l.gasoline!!, 0.001)
        assertNull(l.ethanol)
        assertEquals(FuelMix.GASOLINE, FuelConsumption.mixOf(l.ethanol, l.gasoline))
    }

    @Test
    fun `flex com os dois soma etanol e gasolina`() {
        val l = FuelEntry.liters(true, FuelChoice.BOTH, null, 20.0, 15.0).getOrThrow()
        assertEquals(35.0, l.total, 0.001)
        assertEquals(FuelMix.MIXED, FuelConsumption.mixOf(l.ethanol, l.gasoline))
    }

    @Test
    fun `flex com os dois exige os litros de cada combustivel`() {
        assertTrue(FuelEntry.liters(true, FuelChoice.BOTH, null, 20.0, null).isFailure)
    }

    @Test
    fun `veiculo nao flex usa so o total`() {
        val l = FuelEntry.liters(false, null, 50.0, null, null).getOrThrow()
        assertEquals(50.0, l.total, 0.001)
        assertNull(l.ethanol)
        assertNull(l.gasoline)
    }

    @Test
    fun `litros zerados sao recusados`() {
        assertTrue(FuelEntry.liters(true, FuelChoice.ETHANOL, 0.0, null, null).isFailure)
        assertTrue(FuelEntry.liters(false, null, null, null, null).isFailure)
    }

    @Test
    fun `varios comprovantes ficam no mesmo campo e voltam na mesma ordem`() {
        val raw = ReceiptPaths.join(listOf("/a/bomba.jpg", "/a/painel.jpg", "/a/nota.pdf"))
        assertEquals(listOf("/a/bomba.jpg", "/a/painel.jpg", "/a/nota.pdf"), ReceiptPaths.split(raw))
        assertEquals(3, ReceiptPaths.count(raw))
    }

    @Test
    fun `registro antigo com um so comprovante continua valido`() {
        assertEquals(listOf("/a/antigo.jpg"), ReceiptPaths.split("/a/antigo.jpg"))
    }

    @Test
    fun `sem comprovante o campo fica vazio`() {
        assertNull(ReceiptPaths.join(emptyList()))
        assertEquals(0, ReceiptPaths.count(null))
    }
}
