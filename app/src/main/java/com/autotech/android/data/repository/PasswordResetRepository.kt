package com.autotech.android.data.repository

import android.content.Context
import com.autotech.android.data.local.dao.UserDao
import com.autotech.android.data.remote.email.EmailJsApi
import com.autotech.android.data.remote.email.EmailJsKeys
import com.autotech.android.data.remote.email.EmailJsRequest
import com.autotech.android.domain.CredentialRules
import com.autotech.android.domain.PasswordResetRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

/**
 * RF-01: "Esqueci minha senha". Gera um código de 6 números, envia ao e-mail cadastrado pelo
 * EmailJS e, com o código correto, troca a senha. O pedido em andamento fica na área privada do
 * aplicativo, com o código guardado só como hash (RNF-02).
 */
class PasswordResetRepository(
    context: Context,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val api: EmailJsApi,
    private val keys: EmailJsKeys
) {
    private val prefs = context.getSharedPreferences("autotech_password_reset", Context.MODE_PRIVATE)

    private fun load(email: String): PasswordResetRules.Pending? {
        val hash = prefs.getString("hash_$email", null) ?: return null
        return PasswordResetRules.Pending(hash, prefs.getLong("sent_$email", 0L), prefs.getInt("tries_$email", 0))
    }

    private fun clear(email: String) {
        prefs.edit().remove("hash_$email").remove("tries_$email").apply()
    }

    /** Segundos até poder pedir outro código para o e-mail. */
    fun secondsUntilResend(rawEmail: String, now: Long = System.currentTimeMillis()): Long {
        val email = CredentialRules.normalizeEmail(rawEmail)
        return PasswordResetRules.secondsUntilResend(prefs.getLong("sent_$email", 0L).takeIf { it > 0 }, now)
    }

    suspend fun requestCode(rawEmail: String): Result<Unit> {
        val email = CredentialRules.normalizeEmail(rawEmail)
        if (!CredentialRules.isValidEmail(email)) return fail("Informe um e-mail válido.")
        if (!keys.configured) return fail("O envio de e-mail não está configurado neste aplicativo.")
        // Os dados ficam só neste aparelho; por isso o app pode dizer se a conta existe aqui.
        val user = userDao.getByEmail(email) ?: return fail("Não há conta com esse e-mail neste aparelho.")
        if (user.passwordHash.isEmpty()) {
            return fail("Esta conta foi criada com o Google e não tem senha. Use “Continuar com o Google” para entrar.")
        }
        val now = System.currentTimeMillis()
        val wait = secondsUntilResend(email, now)
        if (wait > 0) return fail("Aguarde $wait segundos para pedir um novo código.")

        val code = PasswordResetRules.newCode()
        val sent = withContext(Dispatchers.IO) {
            runCatching {
                api.send(
                    EmailJsRequest(
                        serviceId = keys.serviceId,
                        templateId = keys.templateId,
                        userId = keys.publicKey,
                        accessToken = keys.privateKey,
                        templateParams = mapOf(
                            "to_email" to email,
                            "nome" to user.displayName.substringBefore(' ').ifBlank { "motorista" },
                            "codigo" to code
                        )
                    )
                ).isSuccessful
            }.getOrDefault(false)
        }
        if (!sent) return fail("Não foi possível enviar o e-mail. Verifique a internet e tente de novo.")

        val hash = withContext(Dispatchers.Default) { BCrypt.hashpw(code, BCrypt.gensalt(10)) }
        prefs.edit()
            .putString("hash_$email", hash)
            .putLong("sent_$email", now)
            .putInt("tries_$email", 0)
            .apply()
        return Result.success(Unit)
    }

    suspend fun resetPassword(rawEmail: String, code: String, newPassword: String, confirmation: String): Result<Unit> {
        val email = CredentialRules.normalizeEmail(rawEmail)
        val pending = load(email)
        when (PasswordResetRules.state(pending, System.currentTimeMillis())) {
            PasswordResetRules.State.NONE -> return fail("Peça um código primeiro.")
            PasswordResetRules.State.EXPIRED -> { clear(email); return fail("O código expirou. Peça um novo.") }
            PasswordResetRules.State.BLOCKED -> { clear(email); return fail("Muitas tentativas erradas. Peça um novo código.") }
            PasswordResetRules.State.VALID -> Unit
        }
        if (!PasswordResetRules.isCodeFormat(code.trim())) return fail("O código tem 6 números.")
        val right = withContext(Dispatchers.Default) { BCrypt.checkpw(code.trim(), pending!!.codeHash) }
        if (!right) {
            val tries = pending!!.attempts + 1
            val left = PasswordResetRules.attemptsLeft(tries)
            return if (left == 0) {
                clear(email)
                fail("Código incorreto. Peça um novo código.")
            } else {
                prefs.edit().putInt("tries_$email", tries).apply()
                fail("Código incorreto. ${if (left == 1) "Resta 1 tentativa." else "Restam $left tentativas."}")
            }
        }
        CredentialRules.validateNewPassword(newPassword, confirmation).onFailure { return Result.failure(it) }
        val user = userDao.getByEmail(email) ?: return fail("Conta não encontrada.")
        // A nova senha precisa ser diferente da atual (o código continua valendo para tentar outra senha)
        val sameAsBefore = user.passwordHash.isNotEmpty() &&
            withContext(Dispatchers.Default) { BCrypt.checkpw(newPassword, user.passwordHash) }
        if (sameAsBefore) return fail("A nova senha precisa ser diferente da senha anterior.")
        authRepository.setNewPassword(user.id, newPassword)
        clear(email)
        return Result.success(Unit)
    }

    private fun fail(message: String): Result<Nothing> = Result.failure(IllegalArgumentException(message))
}
