package com.autotech.android.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** RF-06: agendamento do motor de lembretes (MaintenanceAlertWorker). */
object AlertScheduler {

    private const val PERIODIC = "autotech_maintenance_alerts"
    private const val IMMEDIATE = "autotech_maintenance_alerts_now"
    const val KEY_IMMEDIATE = "imediato"

    /** Verificação a cada 4 horas, mantida pelo Android mesmo após reiniciar o aparelho. */
    fun enqueuePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<MaintenanceAlertWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    /** Verificação logo após uma mudança nos dados (agrupa mudanças seguidas em uma só). */
    fun checkNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<MaintenanceAlertWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(workDataOf(KEY_IMMEDIATE to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.REPLACE, req)
    }
}
