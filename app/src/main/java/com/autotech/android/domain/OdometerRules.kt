package com.autotech.android.domain

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Quilometragem conhecida do veículo em uma data (manutenção ou abastecimento já registrado). */
data class OdometerEvent(val dateMillis: Long, val km: Long)

/**
 * RN-02: KM do evento ≤ KM atual informado no formulário.
 * RN-05: o KM atual não pode ser menor que o maior KM já registrado, e os registros precisam ser
 * coerentes no tempo: um registro numa data não pode ter KM menor que um registro de data anterior
 * nem maior que um registro de data posterior.
 */
object OdometerRules {

    private val ptBr = Locale("pt", "BR")

    fun validateEventVsCurrent(eventKm: Long, currentKm: Long): Result<Unit> {
        if (eventKm > currentKm) {
            return Result.failure(
                IllegalArgumentException("O KM do evento não pode ser maior que o KM atual do veículo.")
            )
        }
        return Result.success(Unit)
    }

    fun validateNoOdometerRegression(newCurrentKm: Long, maxHistoricalKm: Long): Result<Unit> {
        if (newCurrentKm < maxHistoricalKm) {
            return Result.failure(
                IllegalArgumentException(
                    "O KM atual não pode ser menor que o maior KM já registrado (${km(maxHistoricalKm)} km)."
                )
            )
        }
        return Result.success(Unit)
    }

    /**
     * O KM atual do veículo não é obrigatório no formulário. Mas um KM de evento acima do odômetro
     * registrado só é aceito se o usuário confirmar que é o KM atual do carro.
     */
    fun validateAgainstOdometer(eventKm: Long, odometerKm: Long, newCurrentKm: Long?): Result<Unit> {
        if (eventKm > odometerKm && (newCurrentKm == null || newCurrentKm < eventKm)) {
            return Result.failure(
                IllegalArgumentException(
                    "O KM informado (${km(eventKm)} km) é maior que o odômetro registrado (${km(odometerKm)} km). " +
                        "Se for o KM atual do carro, marque “Este é o KM atual do veículo”."
                )
            )
        }
        return Result.success(Unit)
    }

    /** RN-05: coerência no tempo com os registros já existentes (registros do mesmo dia não se comparam). */
    fun validateTimeline(dateMillis: Long, eventKm: Long, events: List<OdometerEvent>): Result<Unit> {
        val day = dayStart(dateMillis)
        val before = events.filter { dayStart(it.dateMillis) < day }.maxByOrNull { it.km }
        if (before != null && before.km > eventKm) {
            return Result.failure(
                IllegalArgumentException(
                    "Em ${date(before.dateMillis)} o veículo já estava com ${km(before.km)} km. " +
                        "Um registro de ${date(dateMillis)} não pode ter menos quilometragem que isso."
                )
            )
        }
        val after = events.filter { dayStart(it.dateMillis) > day }.minByOrNull { it.km }
        if (after != null && after.km < eventKm) {
            return Result.failure(
                IllegalArgumentException(
                    "Em ${date(after.dateMillis)} o veículo estava com ${km(after.km)} km. " +
                        "Um registro de ${date(dateMillis)} não pode ter mais quilometragem que isso."
                )
            )
        }
        return Result.success(Unit)
    }

    private fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun km(value: Long) = NumberFormat.getIntegerInstance(ptBr).format(value)
    private fun date(millis: Long) = SimpleDateFormat("dd/MM/yyyy", ptBr).format(Date(millis))
}
