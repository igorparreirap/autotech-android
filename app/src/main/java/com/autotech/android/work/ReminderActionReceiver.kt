package com.autotech.android.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.autotech.android.AutoTechApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * RF-06: botões da notificação.
 * "Já fiz": o item fica pendente com a data da notificação; o usuário completa os dados depois.
 * "Lembrar amanhã": o mesmo aviso volta 24 horas depois.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? AutoTechApplication ?: return
        val vehicleId = intent.getLongExtra(EXTRA_VEHICLE, -1L)
        val itemKey = intent.getStringExtra(EXTRA_ITEM)
        val key = intent.getStringExtra(EXTRA_KEY)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION, 0)
        val postedAt = intent.getLongExtra(EXTRA_WHEN, System.currentTimeMillis())
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> if (vehicleId > 0 && itemKey != null) {
                        app.graph.maintenanceRepository.markDone(vehicleId, itemKey, postedAt)
                    }
                    ACTION_SNOOZE -> if (key != null) {
                        ReminderStore(app).snooze(key, System.currentTimeMillis() + SNOOZE_MILLIS)
                    }
                }
                NotificationManagerCompat.from(context).cancel(notificationId)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.autotech.android.action.JA_FIZ"
        const val ACTION_SNOOZE = "com.autotech.android.action.LEMBRAR_AMANHA"
        const val EXTRA_VEHICLE = "vehicleId"
        const val EXTRA_ITEM = "itemKey"
        const val EXTRA_KEY = "reminderKey"
        const val EXTRA_NOTIFICATION = "notificationId"
        const val EXTRA_WHEN = "postedAt"
        const val SNOOZE_MILLIS = 24L * 60 * 60 * 1000
    }
}
