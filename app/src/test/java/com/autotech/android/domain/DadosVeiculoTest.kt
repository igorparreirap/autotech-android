package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CT09: combustível informado pela consulta de placa e nome do veículo (RF-02, RF-03). */
class DadosVeiculoTest {

    @Test
    fun `texto ALCOOL barra GASOLINA vira FLEX`() {
        assertEquals(FuelType.FLEX, FuelType.from("ALCOOL / GASOLINA"))
    }

    @Test
    fun `texto Alcool barra Gasolina vira FLEX`() {
        assertEquals(FuelType.FLEX, FuelType.from("Álcool/Gasolina"))
    }

    @Test
    fun `texto FLEX vira FLEX`() {
        assertEquals(FuelType.FLEX, FuelType.from("FLEX"))
    }

    @Test
    fun `texto GASOLINA barra ETANOL vira FLEX`() {
        assertEquals(FuelType.FLEX, FuelType.from("GASOLINA/ETANOL"))
    }

    @Test
    fun `texto GASOLINA vira GASOLINE`() {
        assertEquals(FuelType.GASOLINE, FuelType.from("GASOLINA"))
    }

    @Test
    fun `texto Diesel vira DIESEL`() {
        assertEquals(FuelType.DIESEL, FuelType.from("Diesel"))
    }

    @Test
    fun `texto DIESEL S10 vira DIESEL`() {
        assertEquals(FuelType.DIESEL, FuelType.from("DIESEL S10"))
    }

    @Test
    fun `texto GASOLINA barra ELETRICO vira HYBRID`() {
        assertEquals(FuelType.HYBRID, FuelType.from("GASOLINA/ELETRICO"))
    }

    @Test
    fun `texto HIBRIDO vira HYBRID`() {
        assertEquals(FuelType.HYBRID, FuelType.from("HIBRIDO"))
    }

    @Test
    fun `texto Hibrido flex vira HYBRID`() {
        assertEquals(FuelType.HYBRID, FuelType.from("Híbrido flex"))
    }

    @Test
    fun `texto flex vira FLEX`() {
        assertEquals(FuelType.FLEX, FuelType.from("flex"))
    }

    @Test
    fun `texto diesel vira DIESEL`() {
        assertEquals(FuelType.DIESEL, FuelType.from("diesel"))
    }

    @Test
    fun `texto nulo nao e reconhecido e o usuario escolhe`() {
        assertNull(FuelType.from(null))
    }

    @Test
    fun `texto vazio nao e reconhecido e o usuario escolhe`() {
        assertNull(FuelType.from(""))
    }

    @Test
    fun `texto ELETRICO nao e reconhecido e o usuario escolhe`() {
        assertNull(FuelType.from("ELETRICO"))
    }

    @Test
    fun `texto GNV nao e reconhecido e o usuario escolhe`() {
        assertNull(FuelType.from("GNV"))
    }

    @Test
    fun `valor gravado FLEX e lido de volta`() {
        assertEquals(FuelType.FLEX, FuelType.from(FuelType.FLEX.name))
    }

    @Test
    fun `valor gravado GASOLINE e lido de volta`() {
        assertEquals(FuelType.GASOLINE, FuelType.from(FuelType.GASOLINE.name))
    }

    @Test
    fun `valor gravado DIESEL e lido de volta`() {
        assertEquals(FuelType.DIESEL, FuelType.from(FuelType.DIESEL.name))
    }

    @Test
    fun `valor gravado HYBRID e lido de volta`() {
        assertEquals(FuelType.HYBRID, FuelType.from(FuelType.HYBRID.name))
    }

    @Test
    fun `sem apelido o nome e marca e modelo`() {
        assertEquals("VW Gol", VehicleNaming.display(null, "VW", "Gol"))
    }

    @Test
    fun `apelido substitui o nome`() {
        assertEquals("Carro da Ana", VehicleNaming.display("Carro da Ana", "VW", "Gol"))
    }

    @Test
    fun `apelido vazio volta ao nome original`() {
        assertNull(VehicleNaming.normalizeNickname("   ", "VW", "Gol"))
    }

    @Test
    fun `apelido igual ao original volta ao nome original`() {
        assertNull(VehicleNaming.normalizeNickname("vw gol", "VW", "Gol"))
    }

    @Test
    fun `apelido e limitado a 40 caracteres`() {
        assertEquals(40, VehicleNaming.normalizeNickname("x".repeat(60), "VW", "Gol")!!.length)
    }
}
