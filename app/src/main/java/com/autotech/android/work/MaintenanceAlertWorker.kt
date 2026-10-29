package com.autotech.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.mapper.toCycleInputs
import com.autotech.android.domain.ReminderSchedule
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * RF-06 / RN-01 / RN-03 / RN-04: motor de lembretes.
 * Roda periodicamente em segundo plano (avisos por tempo, mesmo com o app fechado) e logo depois
 * de qualquer mudança de odômetro, manutenção ou abastecimento (avisos por quilometragem).
 */
class MaintenanceAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AutoTechApplication ?: return Result.success()
        if (app.currentUserId == null) app.restoreSession()
        val uid = app.currentUserId ?: return Result.success()

        // Verificação periódica respeita o horário de silêncio (22h às 8h); a verificação feita
        // logo após o usuário mexer no app não espera, pois ele está com o celular na mão.
        val immediate = inputData.getBoolean(AlertScheduler.KEY_IMMEDIATE, false)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (!immediate && hour !in QUIET_END until QUIET_START) return Result.success()
        if (!ReminderNotifier.canPost(app)) return Result.success()

        val store = ReminderStore(app)
        val now = System.currentTimeMillis()
        for (vehicle in app.graph.vehicleRepository.listVehicles(uid)) {
            val cycles = app.graph.maintenanceRepository.observeCycles(vehicle.id).first()
            val records = app.graph.maintenanceRepository.listForPdf(vehicle.id)
            // itens marcados como "Já fiz" e ainda sem os dados não geram avisos
            val pending = app.graph.maintenanceRepository.listPending(vehicle.id).map { it.itemKey }.toSet()
            for (r in ReminderSchedule.due(vehicle.currentOdometerKm, toCycleInputs(cycles, records), now, pending)) {
                val key = ReminderNotifier.key(vehicle.id, r)
                // nova etapa (RN-03) ou fim do "Lembrar amanhã"
                if (ReminderSchedule.shouldNotify(r.stage, store.lastStage(key)) || store.snoozeDue(key, now)) {
                    ReminderNotifier.post(app, vehicle, r)
                    store.save(key, r.stage, now)
                    store.clearSnooze(key)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val QUIET_END = 8
        const val QUIET_START = 22
    }
}
