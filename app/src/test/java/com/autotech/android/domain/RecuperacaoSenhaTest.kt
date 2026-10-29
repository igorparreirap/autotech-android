package com.autotech.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mindrot.jbcrypt.BCrypt

/** CT07: regras da recuperação de senha (RF-01, RNF-02). */
class RecuperacaoSenhaTest {

    private val R = PasswordResetRules
    private val t0 = 1_000_000L
    private val senhaAtual = BCrypt.hashpw("minhaSenha1", BCrypt.gensalt(12))

    @Test
    fun `codigo tem 6 numeros`() {
        assertTrue(R.isCodeFormat(R.newCode(java.util.Random(7))))
    }

    @Test
    fun `codigos gerados variam`() {
        assertTrue((1..50).map { R.newCode() }.toSet().size > 40)
    }

    @Test
    fun `codigo com letra ou com 5 numeros e invalido`() {
        assertTrue(!R.isCodeFormat("12a456") && !R.isCodeFormat("12345"))
    }

    @Test
    fun `sem pedido o estado e NONE`() {
        assertEquals(PasswordResetRules.State.NONE, R.state(null, t0))
    }

    @Test
    fun `codigo 10 minutos depois e valido`() {
        assertEquals(PasswordResetRules.State.VALID, R.state(PasswordResetRules.Pending("h", t0, 0), t0 + 10 * 60_000))
    }

    @Test
    fun `codigo 16 minutos depois esta expirado`() {
        assertEquals(PasswordResetRules.State.EXPIRED, R.state(PasswordResetRules.Pending("h", t0, 0), t0 + 16 * 60_000))
    }

    @Test
    fun `5 tentativas erradas bloqueiam o codigo`() {
        assertEquals(PasswordResetRules.State.BLOCKED, R.state(PasswordResetRules.Pending("h", t0, 5), t0 + 1000))
    }

    @Test
    fun `reenvio 20 segundos depois espera mais 40 segundos`() {
        assertEquals(40L, R.secondsUntilResend(t0, t0 + 20_000))
    }

    @Test
    fun `reenvio 61 segundos depois e liberado`() {
        assertEquals(0L, R.secondsUntilResend(t0, t0 + 61_000))
    }

    @Test
    fun `nova senha curta e recusada`() {
        assertTrue(CredentialRules.validateNewPassword("123", "123").isFailure)
    }

    @Test
    fun `senha e confirmacao diferentes sao recusadas`() {
        assertTrue(CredentialRules.validateNewPassword("abcdef", "abcdeg").isFailure)
    }

    @Test
    fun `nova senha valida e aceita`() {
        assertTrue(CredentialRules.validateNewPassword("abcdef", "abcdef").isSuccess)
    }

    @Test
    fun `formato do e-mail e validado`() {
        assertTrue(CredentialRules.isValidEmail(" Igor@Gmail.com ") && !CredentialRules.isValidEmail("igor@gmail"))
    }

    @Test
    fun `nova senha igual a anterior e reconhecida`() {
        assertTrue(BCrypt.checkpw("minhaSenha1", senhaAtual))
    }

    @Test
    fun `nova senha diferente da anterior e aceita`() {
        assertTrue(!BCrypt.checkpw("outraSenha2", senhaAtual))
    }

    @Test
    fun `diferenca so de maiuscula conta como senha nova`() {
        assertTrue(!BCrypt.checkpw("MinhaSenha1", senhaAtual))
    }
}
