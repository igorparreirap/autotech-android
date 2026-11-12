package com.autotech.android.domain

/** RF-04: o que o usuário informou para um item de uma manutenção com várias peças ou serviços. */
data class ItemEntry(val key: String, val value: Double?, val note: String?)

/** Registro que será gravado para um item: [amount] já inclui a mão de obra quando o item é o principal. */
data class ItemRecord(val key: String, val amount: Double?, val labor: Double?, val description: String?)

/**
 * RF-04: divide uma manutenção em um registro por item.
 *
 * Cada peça ou serviço guarda o próprio valor e a própria observação (marca, loja...). A mão de obra
 * é do atendimento inteiro e fica no item principal, junto com a descrição geral; assim a soma dos
 * registros é igual ao total pago e o painel financeiro não conta nada duas vezes.
 */
object MaintenanceSplit {

    /** Registros na ordem dos itens; o primeiro é o principal. */
    fun records(items: List<ItemEntry>, labor: Double?, general: String?, sameServiceNote: String?): List<ItemRecord> {
        val distinct = items.distinctBy { it.key }
        return distinct.mapIndexed { i, item ->
            if (i == 0) {
                ItemRecord(
                    key = item.key,
                    amount = MaintenanceCost.total(item.value, labor),
                    labor = labor,
                    description = join(item.note, general)
                )
            } else {
                ItemRecord(
                    key = item.key,
                    amount = item.value,
                    labor = null,
                    description = join(item.note, sameServiceNote)
                )
            }
        }
    }

    /** Total pago no atendimento: soma das peças e da mão de obra; null se nada foi informado. */
    fun total(items: List<ItemEntry>, labor: Double?): Double? {
        val values = items.distinctBy { it.key }.mapNotNull { it.value }
        val parts = if (values.isEmpty()) null else values.sum()
        return MaintenanceCost.total(parts, labor)
    }

    /** Junta duas descrições em linhas separadas, ignorando as vazias. */
    fun join(first: String?, second: String?): String? =
        listOfNotNull(first?.trim()?.ifBlank { null }, second?.trim()?.ifBlank { null })
            .joinToString("\n")
            .ifBlank { null }
}
