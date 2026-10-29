package com.autotech.android.data.repository

import com.autotech.android.data.local.dao.UserDao
import com.autotech.android.data.local.entity.UserEntity
import com.autotech.android.domain.CredentialRules
import org.mindrot.jbcrypt.BCrypt

/**
 * RNF-02: fator de custo do BCrypt (2^12 rodadas). Hashes antigos, gerados com outro custo,
 * continuam válidos no login, porque o custo fica gravado dentro do próprio hash.
 */
private const val BCRYPT_COST = 12

class AuthRepository(private val userDao: UserDao) {

    suspend fun register(email: String, password: String, displayName: String): Result<Long> {
        CredentialRules.validateRegister(displayName, email, password).onFailure { return Result.failure(it) }
        val normalized = CredentialRules.normalizeEmail(email)
        if (userDao.getByEmail(normalized) != null) {
            return Result.failure(IllegalStateException("E-mail já cadastrado."))
        }
        val hash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST))
        val id = userDao.insert(
            UserEntity(
                email = normalized,
                passwordHash = hash,
                displayName = displayName.trim(),
                authProvider = "email"
            )
        )
        return Result.success(id)
    }

    suspend fun login(email: String, password: String): Result<Long> {
        CredentialRules.validateLogin(email, password).onFailure { return Result.failure(it) }
        val normalized = CredentialRules.normalizeEmail(email)
        val user = userDao.getByEmail(normalized)
            ?: return Result.failure(IllegalArgumentException("Credenciais inválidas."))
        if (user.authProvider != "email" || user.passwordHash.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Esta conta usa login social. Use o botão 'Continuar com Google'.")
            )
        }
        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Credenciais inválidas."))
        }
        return Result.success(user.id)
    }

    /**
     * Upsert por e-mail: se o usuário já existe (seja via e-mail ou Google), devolve o id
     * existente; caso contrário, cria um novo registro com authProvider = "google".
     */
    suspend fun signInWithGoogle(email: String, displayName: String): Result<Long> {
        val normalized = email.trim().lowercase()
        val existing = userDao.getByEmail(normalized)
        if (existing != null) {
            return Result.success(existing.id)
        }
        val id = userDao.insert(
            UserEntity(
                email = normalized,
                passwordHash = "",
                displayName = displayName.trim().ifBlank { normalized },
                authProvider = "google"
            )
        )
        return Result.success(id)
    }

    suspend fun getUser(id: Long): UserEntity? = userDao.getById(id)

    /** RF-01 / RNF-02: grava a nova senha da recuperação, também em hash BCrypt. */
    suspend fun setNewPassword(userId: Long, password: String) {
        userDao.updatePassword(userId, BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST)))
    }
}
