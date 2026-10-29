package com.autotech.android.data.mapper

import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.domain.CycleInput
import com.autotech.android.domain.Expense
import com.autotech.android.domain.MaintenanceCatalog

/*
 * Camada de dados -> domínio (RNF-01): converte os registros do banco nos modelos
 * usados pelas regras de negócio. O domínio não conhece as classes do banco.
 */

/** Junta cada ciclo ao serviço que serve de marco zero (RN-04). Ciclos sem serviço são ignorados. */
fun toCycleInputs(cycles: List<MaintenanceCycleEntity>, records: List<MaintenanceRecordEntity>): List<CycleInput> {
    val byId = records.associateBy { it.id }
    return cycles.mapNotNull { c ->
        byId[c.linkedMaintenanceId]?.let { r ->
            CycleInput(c.itemKey, c.intervalKm, c.intervalMonths, r.eventKm, r.eventDateMillis)
        }
    }
}

fun MaintenanceRecordEntity.toExpense(): Expense =
    Expense(vehicleId, eventDateMillis, eventKm, amount ?: 0.0, MaintenanceCatalog.categoryLabel(itemKey), false, 0.0, itemKey)

fun FuelRecordEntity.toExpense(): Expense =
    Expense(vehicleId, dateMillis, eventKm, amount ?: 0.0, "Combustível", true, liters, null)

/** RF-05: abastecimento visto pelo cálculo de consumo (domain/FuelConsumption). */
fun FuelRecordEntity.toFillUp(): com.autotech.android.domain.FillUp =
    com.autotech.android.domain.FillUp(id, dateMillis, eventKm, liters, ethanolLiters, gasolineLiters, computeConsumption)
