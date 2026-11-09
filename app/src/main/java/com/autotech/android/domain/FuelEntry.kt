package com.autotech.android.domain

/** RF-05: combustível colocado no abastecimento de um veículo flex. */
enum class FuelChoice(val label: String) {
    ETHANOL("Etanol"),
    GASOLINE("Gasolina"),
    BOTH("Os dois")
}

/** Litros totais e, quando se sabe, quanto foi de etanol e quanto foi de gasolina. */
data class FuelLiters(val total: Double, val ethanol: Double?, val gasoline: Double?)

/**
 * RF-05: monta os litros do abastecimento. Em veículo flex o combustível é obrigatório, para que o
 * consumo saiba com o que o carro rodou: só etanol, só gasolina ou os dois (com os litros de cada um).
 * Nos demais veículos vale só o total.
 */
object FuelEntry {

    fun liters(isFlex: Boolean, choice: FuelChoice?, liters: Double?, ethanol: Double?, gasoline: Double?): Result<FuelLiters> {
        if (!isFlex) {
            val l = liters?.takeIf { it > 0 } ?: return fail("Informe os litros.")
            return Result.success(FuelLiters(l, null, null))
        }
        return when (choice) {
            null -> fail("Escolha o combustível: etanol, gasolina ou os dois.")
            FuelChoice.ETHANOL -> liters?.takeIf { it > 0 }?.let { Result.success(FuelLiters(it, it, null)) } ?: fail("Informe os litros.")
            FuelChoice.GASOLINE -> liters?.takeIf { it > 0 }?.let { Result.success(FuelLiters(it, null, it)) } ?: fail("Informe os litros.")
            FuelChoice.BOTH -> {
                val e = ethanol?.takeIf { it > 0 }
                val g = gasoline?.takeIf { it > 0 }
                if (e == null || g == null) fail("Informe os litros de etanol e os de gasolina.")
                else Result.success(FuelLiters(e + g, e, g))
            }
        }
    }

    private fun fail(message: String): Result<FuelLiters> = Result.failure(IllegalArgumentException(message))
}

/**
 * RF-04 / RF-05: um registro pode ter vários comprovantes (fotos da bomba, do painel, nota fiscal...).
 * Os caminhos ficam juntos no mesmo campo, um por linha; registros antigos, com um só caminho, continuam válidos.
 */
object ReceiptPaths {

    private const val SEPARATOR = "\n"

    fun split(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    fun join(paths: List<String>): String? =
        paths.map { it.trim() }.filter { it.isNotEmpty() }.distinct().takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

    fun count(raw: String?): Int = split(raw).size
}
