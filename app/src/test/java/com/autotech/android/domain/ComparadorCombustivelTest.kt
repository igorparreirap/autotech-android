package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT05: comparação de custo entre etanol e gasolina (RF-13, RN-06). */
class ComparadorCombustivelTest {

    @Test
    fun `etanol a 66 por cento da gasolina compensa`() {
        assertEquals(FuelComparison.Verdict.ETHANOL, FuelComparison.compare(6.0, 4.0)?.verdict)
    }

    @Test
    fun `etanol a 75 por cento da gasolina nao compensa`() {
        assertEquals(FuelComparison.Verdict.GASOLINE, FuelComparison.compare(6.0, 4.5)?.verdict)
    }

    @Test
    fun `etanol a 70 por cento da gasolina tanto faz`() {
        assertEquals(FuelComparison.Verdict.EQUIVALENT, FuelComparison.compare(6.0, 4.2)?.verdict)
    }

    @Test
    fun `preco maximo do etanol e 70 por cento da gasolina`() {
        assertEquals(4.2, FuelComparison.compare(6.0, 4.0)!!.maxEthanolPrice, 0.01)
    }

    @Test
    fun `sem os dois precos nao ha resultado`() {
        assertTrue(FuelComparison.compare(6.0, null) == null && FuelComparison.compare(0.0, 4.0) == null)
    }
}
