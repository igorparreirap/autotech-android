package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT04: cálculo de consumo pelo método tanque a tanque (RF-05). */
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
    fun `media com alcool de 10 km por litro`() {
        assertTrue(near(resumo.ethanol, 10.0))
    }

    @Test
    fun `media com gasolina de 12 km por litro`() {
        assertTrue(near(resumo.gasoline, 12.0))
    }

    @Test
    fun `alcool rende 83 por cento da gasolina neste carro`() {
        assertTrue(near(resumo.ethanolVsGasoline, 10.0 / 12.0))
    }

    @Test
    fun `mistura de alcool e gasolina e MIXED`() {
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
}
