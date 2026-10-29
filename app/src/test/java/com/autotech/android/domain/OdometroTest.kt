package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT03: consistência e integridade temporal do odômetro (RN-02, RN-05) e lista da revisão. */
class OdometroTest {

    private fun d(m: Int, day: Int = 10) = java.util.Calendar.getInstance().apply { set(2026, m - 1, day, 12, 0, 0) }.timeInMillis
    private val eventos = listOf(OdometerEvent(d(1), 120_000), OdometerEvent(d(10), 180_000))

    @Test
    fun `marco com 50000 km depois de janeiro com 120000 km e recusado`() {
        val r = OdometerRules.validateTimeline(d(3), 50_000, eventos)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("120.000 km"))
    }

    @Test
    fun `marco com 150000 km entre janeiro e outubro e aceito`() {
        assertTrue(OdometerRules.validateTimeline(d(3), 150_000, eventos).isSuccess)
    }

    @Test
    fun `marco com 190000 km acima de outubro com 180000 km e recusado`() {
        val r = OdometerRules.validateTimeline(d(3), 190_000, eventos)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("180.000 km"))
    }

    @Test
    fun `registros do mesmo dia nao se comparam`() {
        assertTrue(OdometerRules.validateTimeline(d(1, 10), 119_000, listOf(OdometerEvent(d(1, 10), 120_000))).isSuccess)
    }

    @Test
    fun `sem registros anteriores o lancamento e aceito`() {
        assertTrue(OdometerRules.validateTimeline(d(3), 1, emptyList()).isSuccess)
    }

    @Test
    fun `servico abaixo do odometro sem KM atual e aceito`() {
        assertTrue(OdometerRules.validateAgainstOdometer(150_000, 180_000, null).isSuccess)
    }

    @Test
    fun `KM acima do odometro sem marcar KM atual e recusado`() {
        val r = OdometerRules.validateAgainstOdometer(185_000, 180_000, null)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("Este é o KM atual do veículo"))
    }

    @Test
    fun `KM acima do odometro marcado como KM atual e aceito`() {
        assertTrue(OdometerRules.validateAgainstOdometer(185_000, 180_000, 185_000).isSuccess)
    }

    @Test
    fun `KM atual menor que o do servico e recusado`() {
        assertTrue(OdometerRules.validateAgainstOdometer(185_000, 180_000, 184_000).isFailure)
    }

    @Test
    fun `revisao de veiculo flex tem oleo, filtros e velas`() {
        assertTrue(MaintenanceCatalog.reviewChecklist(FuelType.FLEX).map { it.key }.containsAll(listOf("oil", "air_filter", "fuel_filter", "spark_plugs")))
    }

    @Test
    fun `revisao de veiculo diesel troca velas por separador de agua`() {
        assertTrue(MaintenanceCatalog.reviewChecklist(FuelType.DIESEL).map { it.key }.let { "spark_plugs" !in it && "water_separator" in it })
    }
}
