package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT02: escalonamento dos avisos e marcação "Já fiz" (RF-06, RN-03, RN-04). */
class AvisosManutencaoTest {

    private val now = System.currentTimeMillis()
    private val day = 86_400_000L
    private fun ago(days: Long) = now - days * day
    private val t = listOf(30L, 15L, 7L)
    private val oneYearAgoPlus30 = java.util.Calendar.getInstance().apply {
        timeInMillis = now; add(java.util.Calendar.YEAR, -1); add(java.util.Calendar.DAY_OF_YEAR, 30)
    }.timeInMillis
    private val pendentes = listOf(
        CycleInput("oil", 10_000, 12, 10_000, now - 380 * day),
        CycleInput("brake", 20_000, null, 1_000, now - 10 * day))

    @Test
    fun `ciclo de 12 meses avisa a 30, 15 e 7 dias`() {
        assertEquals(listOf(30L, 15L, 7L), ReminderSchedule.dayThresholds(365))
    }

    @Test
    fun `ciclo de 6 meses avisa a 15, 8 e 4 dias`() {
        assertEquals(listOf(15L, 8L, 4L), ReminderSchedule.dayThresholds(183))
    }

    @Test
    fun `ciclo de 3 meses avisa a 8, 4 e 2 dias`() {
        assertEquals(listOf(8L, 4L, 2L), ReminderSchedule.dayThresholds(92))
    }

    @Test
    fun `ciclo de 1 mes avisa a 2 e 1 dia sem repetir`() {
        assertEquals(listOf(2L, 1L), ReminderSchedule.dayThresholds(30))
    }

    @Test
    fun `ciclo de 24 meses avisa a 60, 30 e 14 dias`() {
        assertEquals(listOf(60L, 30L, 14L), ReminderSchedule.dayThresholds(730))
    }

    @Test
    fun `ciclo de 10000 km avisa faltando 5000, 1000 e 500 km`() {
        assertEquals(listOf(5_000L, 1_000L, 500L), ReminderSchedule.kmThresholds(10_000))
    }

    @Test
    fun `ciclo de 40000 km avisa faltando 20000, 4000 e 2000 km`() {
        assertEquals(listOf(20_000L, 4_000L, 2_000L), ReminderSchedule.kmThresholds(40_000))
    }

    @Test
    fun `faltando 40 dias nao ha aviso`() {
        assertNull(ReminderSchedule.stageFor(40, t))
    }

    @Test
    fun `faltando 30 dias entra na etapa 30`() {
        assertEquals(30L, ReminderSchedule.stageFor(30, t))
    }

    @Test
    fun `faltando 20 dias continua na etapa 30`() {
        assertEquals(30L, ReminderSchedule.stageFor(20, t))
    }

    @Test
    fun `faltando 10 dias entra na etapa 15`() {
        assertEquals(15L, ReminderSchedule.stageFor(10, t))
    }

    @Test
    fun `faltando 3 dias entra na etapa 7`() {
        assertEquals(7L, ReminderSchedule.stageFor(3, t))
    }

    @Test
    fun `ciclo vencido entra na etapa vencido`() {
        assertEquals(ReminderSchedule.OVERDUE, ReminderSchedule.stageFor(0, t))
    }

    @Test
    fun `primeira vez na etapa 30 avisa`() {
        assertTrue(ReminderSchedule.shouldNotify(30, null))
    }

    @Test
    fun `etapa 30 de novo no dia seguinte nao avisa`() {
        assertTrue(!ReminderSchedule.shouldNotify(30, 30))
    }

    @Test
    fun `passagem para a etapa 15 avisa`() {
        assertTrue(ReminderSchedule.shouldNotify(15, 30))
    }

    @Test
    fun `app fechado por semanas ja na etapa 7 gera um unico aviso`() {
        assertTrue(ReminderSchedule.shouldNotify(7, null))
    }

    @Test
    fun `depois de vencido nao avisa de novo`() {
        assertTrue(!ReminderSchedule.shouldNotify(0, 0))
    }

    @Test
    fun `oleo faltando 30 dias e 8000 km gera so o aviso de tempo`() {
        val r = ReminderSchedule.due(12_000, listOf(CycleInput("oil", 10_000, 12, 10_000, oneYearAgoPlus30)), now)
        assertTrue(r.size == 1 && r[0].track == ReminderSchedule.Track.TIME && r[0].stage == 30L)
    }

    @Test
    fun `oleo com 5000 km rodados gera aviso de km faltando 5000`() {
        val r = ReminderSchedule.due(15_000, listOf(CycleInput("oil", 10_000, 12, 10_000, ago(10))), now)
        assertTrue(r.size == 1 && r[0].track == ReminderSchedule.Track.KM && r[0].stage == 5_000L && r[0].left == 5_000L)
    }

    @Test
    fun `oleo faltando 400 km entra na etapa 500 km`() {
        assertEquals(500L, ReminderSchedule.due(19_600, listOf(CycleInput("oil", 10_000, 12, 10_000, ago(10))), now).single().stage)
    }

    @Test
    fun `km vencido gera so o aviso de vencido`() {
        val r = ReminderSchedule.due(20_100, listOf(CycleInput("oil", 10_000, 12, 10_000, oneYearAgoPlus30)), now)
        assertTrue(r.size == 1 && r[0].overdue && r[0].track == ReminderSchedule.Track.KM)
    }

    @Test
    fun `novo servico registrado vale o ciclo novo sem avisos`() {
        val r = ReminderSchedule.due(15_000, listOf(
            CycleInput("oil", 10_000, 12, 5_000, ago(400)),
            CycleInput("oil", 10_000, 12, 15_000, ago(1))), now)
        assertTrue(r.isEmpty())
    }

    @Test
    fun `RN-04 ciclo sem intervalo nao gera aviso`() {
        assertTrue(ReminderSchedule.due(99_000, listOf(CycleInput("oil", null, null, 0, ago(900))), now).isEmpty())
    }

    @Test
    fun `cada ciclo tem uma chave propria`() {
        val a = ReminderSchedule.due(15_000, listOf(CycleInput("oil", 10_000, 12, 10_000, ago(10))), now)[0].cycleKey
        val b = ReminderSchedule.due(15_000, listOf(CycleInput("oil", 10_000, 12, 10_001, ago(10))), now).firstOrNull()?.cycleKey
        assertTrue(a != b)
    }

    @Test
    fun `sem Ja fiz avisa oleo e freio`() {
        assertEquals(setOf("oil", "brake"), ReminderSchedule.due(20_500, pendentes, now).map { it.itemKey }.toSet())
    }

    @Test
    fun `oleo marcado como Ja fiz deixa so o freio avisar`() {
        assertEquals(listOf("brake"), ReminderSchedule.due(20_500, pendentes, now, skipItems = setOf("oil")).map { it.itemKey })
    }
}
