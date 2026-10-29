package com.autotech.android.domain

/**
 * RF01: validação das credenciais próprias antes de qualquer acesso ao banco.
 * Impede cadastro e login com campos vazios ou e-mail malformado.
 */
object CredentialRules {

    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun isValidEmail(raw: String): Boolean = EMAIL_REGEX.matches(normalizeEmail(raw))

    /** RF-01: nova senha na recuperação (mesmas regras do cadastro + confirmação). */
    fun validateNewPassword(password: String, confirmation: String): Result<Unit> = when {
        password.length < MIN_PASSWORD_LENGTH ->
            Result.failure(IllegalArgumentException("A senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."))
        password.isBlank() ->
            Result.failure(IllegalArgumentException("A senha não pode ser só espaços."))
        password != confirmation ->
            Result.failure(IllegalArgumentException("As duas senhas não são iguais."))
        else -> Result.success(Unit)
    }

    fun validateLogin(email: String, password: String): Result<Unit> = when {
        email.isBlank() || password.isEmpty() ->
            Result.failure(IllegalArgumentException("Informe e-mail e senha."))
        !EMAIL_REGEX.matches(normalizeEmail(email)) ->
            Result.failure(IllegalArgumentException("E-mail inválido."))
        else -> Result.success(Unit)
    }

    fun validateRegister(name: String, email: String, password: String): Result<Unit> = when {
        name.isBlank() ->
            Result.failure(IllegalArgumentException("Informe seu nome."))
        email.isBlank() ->
            Result.failure(IllegalArgumentException("Informe o e-mail."))
        !EMAIL_REGEX.matches(normalizeEmail(email)) ->
            Result.failure(IllegalArgumentException("E-mail inválido."))
        password.length < MIN_PASSWORD_LENGTH ->
            Result.failure(IllegalArgumentException("A senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres."))
        password.isBlank() ->
            Result.failure(IllegalArgumentException("A senha não pode ser só espaços."))
        else -> Result.success(Unit)
    }
}
