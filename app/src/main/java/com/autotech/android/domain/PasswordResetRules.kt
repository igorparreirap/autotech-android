package com.autotech.android.domain

import java.security.SecureRandom
import java.util.Random

/**
 * RF-01: recuperação de senha por código enviado ao e-mail cadastrado.
 * O código tem 6 números, vale 15 minutos, aceita até 5 tentativas e só pode ser
 * reenviado depois de 60 segundos (para não gastar a cota gratuita do serviço de e-mail).
 */
object PasswordResetRules {

    const val CODE_LENGTH = 6
    const val VALIDITY_MILLIS = 15 * 60 * 1000L
    const val MAX_ATTEMPTS = 5
    const val RESEND_COOLDOWN_MILLIS = 60 * 1000L

    /** Pedido em andamento: o código fica guardado só como hash, como a senha. */
    data class Pending(val codeHash: String, val sentAtMillis: Long, val attempts: Int)

    enum class State { NONE, VALID, EXPIRED, BLOCKED }

    fun newCode(random: Random = SecureRandom()): String =
        (1..CODE_LENGTH).joinToString("") { random.nextInt(10).toString() }

    fun isCodeFormat(code: String): Boolean = code.length == CODE_LENGTH && code.all { it.isDigit() }

    fun state(pending: Pending?, now: Long): State = when {
        pending == null -> State.NONE
        now - pending.sentAtMillis > VALIDITY_MILLIS -> State.EXPIRED
        pending.attempts >= MAX_ATTEMPTS -> State.BLOCKED
        else -> State.VALID
    }

    /** Segundos que faltam para poder pedir outro código (0 = já pode). */
    fun secondsUntilResend(lastSentAtMillis: Long?, now: Long): Long {
        if (lastSentAtMillis == null) return 0
        val left = lastSentAtMillis + RESEND_COOLDOWN_MILLIS - now
        return if (left <= 0) 0 else (left + 999) / 1000
    }

    fun attemptsLeft(attempts: Int): Int = (MAX_ATTEMPTS - attempts).coerceAtLeast(0)
}
