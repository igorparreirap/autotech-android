package com.autotech.android.domain

import java.util.Locale

/**
 * RF02: regras da placa usadas antes da consulta externa.
 * Aceita o padrão antigo (ABC1234) e o padrão Mercosul (ABC1D23),
 * com ou sem hífen e em letras minúsculas ou maiúsculas.
 */
object PlateRules {

    private val PLATE_REGEX = Regex("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")

    /** Remove hífen/espaços e converte para maiúsculas: "abc-1d23" -> "ABC1D23". */
    fun normalize(raw: String): String =
        raw.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }

    fun isValid(raw: String): Boolean = PLATE_REGEX.matches(normalize(raw))
}
