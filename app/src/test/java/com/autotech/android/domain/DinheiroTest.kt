package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Valores em dinheiro guardados em centavos (RF-04, RF-05, RF-08). */
class DinheiroTest {

    @Test
    fun `valor com virgula vira centavos`() {
        assertEquals(28_990L, Money.parseCents("289,90"))
        assertEquals(28_990L, Money.parseCents("289,9"))
    }

    @Test
    fun `ponto de milhar e simbolo do real sao aceitos`() {
        assertEquals(127_500L, Money.parseCents("1.275,00"))
        assertEquals(127_500L, Money.parseCents("R$ 1.275"))
        assertEquals(28_990L, Money.parseCents("289.90"))
    }

    @Test
    fun `campo vazio texto invalido ou valor negativo nao tem valor`() {
        assertNull(Money.parseCents(""))
        assertNull(Money.parseCents("abc"))
        assertNull(Money.parseCents("-10,00"))
    }

    @Test
    fun `terceira casa decimal e arredondada`() {
        assertEquals(1_001L, Money.parseCents("10,005"))
    }

    @Test
    fun `soma em centavos nao perde valor`() {
        // em Double, 0.1 + 0.2 = 0.30000000000000004; em centavos a soma é exata
        val soma = listOf("0,10", "0,20").sumOf { Money.parseCents(it)!! }
        assertEquals(30L, soma)
        assertEquals("0.30", Money.toReais(soma).toPlainString())
    }

    @Test
    fun `dez mil lancamentos de 0,10 somam exatamente 1000 reais`() {
        assertEquals(100_000L, List(10_000) { Money.parseCents("0,10")!! }.sum())
    }

    @Test
    fun `reais vindos de Double sao arredondados para o centavo`() {
        assertEquals(28_990L, Money.fromReais(289.9))
    }
}
