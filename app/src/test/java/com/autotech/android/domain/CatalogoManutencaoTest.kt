package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT06: catálogo de itens de manutenção (RF-04). */
class CatalogoManutencaoTest {

    private val C = MaintenanceCatalog

    @Test
    fun `catalogo tem 10 partes do carro`() {
        assertEquals(10, C.categories.size)
    }

    @Test
    fun `toda chave e unica`() {
        assertEquals(C.items.size, C.items.map { it.key }.toSet().size)
    }

    @Test
    fun `todo item pertence a uma parte existente`() {
        assertTrue(C.items.all { i -> C.categories.any { it.key == i.category } })
    }

    @Test
    fun `toda parte tem itens principais`() {
        assertTrue(C.categories.all { C.mainItems(it.key, null).isNotEmpty() })
    }

    @Test
    fun `no motor o oleo e o primeiro item principal`() {
        assertEquals("oil", C.mainItems("motor", FuelType.FLEX).first().key)
    }

    @Test
    fun `motor flex tem velas de ignicao e nao tem velas aquecedoras`() {
        assertTrue(C.visibleItems("motor", FuelType.FLEX).any { it.key == "spark_plugs" } && C.visibleItems("motor", FuelType.FLEX).none { it.key == "glow_plugs" })
    }

    @Test
    fun `motor diesel tem velas aquecedoras e ARLA 32 e nao tem velas de ignicao`() {
        assertTrue(C.visibleItems("motor", FuelType.DIESEL).let { l -> l.any { it.key == "glow_plugs" } && l.any { it.key == "arla" } && l.none { it.key == "spark_plugs" } })
    }

    @Test
    fun `combustivel nao informado mostra todos os itens`() {
        assertTrue(C.visibleItems("motor", null).any { it.key == "glow_plugs" } && C.visibleItems("motor", null).any { it.key == "spark_plugs" })
    }

    @Test
    fun `demais itens em ordem alfabetica`() {
        assertTrue(C.otherItems("motor", null).map { it.label }.let { it == it.sortedBy { s -> java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).lowercase() } })
    }

    @Test
    fun `chaves antigas continuam com nome`() {
        assertTrue(C.label("oil") == "Óleo do motor e filtro" && C.label("brake") == "Revisão dos freios" && C.label("other") == "Outro")
    }

    @Test
    fun `chaves antigas ganham a parte do carro`() {
        assertTrue(C.categoryLabel("oil") == "Motor" && C.categoryLabel("tire") == "Pneus e rodas" && C.categoryLabel("other") == "Outros")
    }

    @Test
    fun `item proprio guarda parte e nome`() {
        val k = C.customKey("motor", "  selo   do motor ")!!
        assertTrue(k == "custom:motor:Selo do motor" && C.label(k) == "Selo do motor" && C.categoryLabel(k) == "Motor")
    }

    @Test
    fun `item proprio sem nome nao vale`() {
        assertNull(C.customKey("motor", "   "))
    }

    @Test
    fun `mesmo nome reaproveita o item ja criado`() {
        val k = C.customKey("motor", "Selo do motor")!!
        assertEquals(k, C.customKey("motor", "SELO DO MOTOR", listOf(k)))
    }

    @Test
    fun `mesmo nome em outra parte e outro item`() {
        val k = C.customKey("motor", "Selo do motor")!!
        assertTrue(C.customKey("suspensao", "Selo do motor", listOf(k)) != k)
    }

    @Test
    fun `busca sem acento encontra oleo`() {
        assertTrue(C.search("oleo", null).any { it.key == "oil" })
    }

    @Test
    fun `busca por pastilha encontra dianteiras e traseiras`() {
        assertEquals(2, C.search("pastilha", null).size)
    }

    @Test
    fun `busca respeita o combustivel`() {
        assertEquals(listOf("glow_plugs"), C.search("vela", FuelType.DIESEL).map { it.key })
    }
}
