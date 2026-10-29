package com.autotech.android.domain

import java.text.Normalizer
import java.util.Locale

/**
 * RF-03: combustível do veículo, escolhido no cadastro. Veículos 100% elétricos não
 * são atendidos, pois o aplicativo controla abastecimento em litros.
 * No banco fica gravado o nome da constante (ex.: "FLEX"); textos antigos vindos da
 * consulta da placa (ex.: "ALCOOL / GASOLINA") continuam sendo reconhecidos.
 */
enum class FuelType(val label: String, val hint: String) {
    FLEX("Flex", "Álcool e gasolina"),
    GASOLINE("Gasolina", "Só gasolina"),
    DIESEL("Diesel", "Diesel S10 ou S500"),
    HYBRID("Híbrido", "Combustão + elétrico");

    companion object {
        fun from(raw: String?): FuelType? {
            if (raw.isNullOrBlank()) return null
            entries.firstOrNull { it.name == raw.trim() }?.let { return it }
            val s = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .uppercase(Locale.ROOT)
            val ethanol = "ALCOOL" in s || "ETANOL" in s
            val gasoline = "GASOLINA" in s
            return when {
                "DIESEL" in s -> DIESEL
                "HIBRID" in s || ("ELETRIC" in s && (gasoline || ethanol || "FLEX" in s)) -> HYBRID
                "FLEX" in s || (ethanol && gasoline) -> FLEX
                gasoline -> GASOLINE
                else -> null
            }
        }
    }
}
