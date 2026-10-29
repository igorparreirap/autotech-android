package com.autotech.android.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.autotech.android.MainActivity
import com.autotech.android.R
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.ReminderSchedule
import com.autotech.android.domain.VehicleNaming
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** RF-06: envio das notificações locais de manutenção. */
object ReminderNotifier {

    const val CHANNEL_ID = "lembretes_manutencao"
    const val EXTRA_VEHICLE_ID = "autotech.open_maintenance_vehicle"

    fun key(vehicleId: Long, r: ReminderSchedule.Reminder) = "$vehicleId:${r.track}:${r.cycleKey}"

    private val ptBr = Locale("pt", "BR")

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Lembretes de manutenção", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos antes do vencimento das manutenções por tempo e por quilometragem"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Permissão concedida (Android 13+) e notificações do app ativadas nas configurações. */
    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // verificado em canPost()
    fun post(context: Context, vehicle: VehicleEntity, r: ReminderSchedule.Reminder) {
        if (!canPost(context)) return
        val name = VehicleNaming.short(vehicle.nickname, vehicle.model)
        val (title, text) = message(name, vehicle.currentOdometerKm, r)

        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_VEHICLE_ID, vehicle.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context, vehicle.id.toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // RF-06: ações na própria notificação
        val key = key(vehicle.id, r)
        val notificationId = key.hashCode()
        fun action(name: String, request: Int) = PendingIntent.getBroadcast(
            context, request,
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(name)
                .putExtra(ReminderActionReceiver.EXTRA_VEHICLE, vehicle.id)
                .putExtra(ReminderActionReceiver.EXTRA_ITEM, r.itemKey)
                .putExtra(ReminderActionReceiver.EXTRA_KEY, key)
                .putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION, notificationId)
                .putExtra(ReminderActionReceiver.EXTRA_WHEN, System.currentTimeMillis()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_autotech)
            .setColor(0xFF1569CC.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (r.overdue) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .addAction(0, "Já fiz", action(ReminderActionReceiver.ACTION_DONE, notificationId * 2))
            .addAction(0, "Lembrar amanhã", action(ReminderActionReceiver.ACTION_SNOOZE, notificationId * 2 + 1))
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /** Título e texto da notificação. */
    fun message(vehicleName: String, currentKm: Long, r: ReminderSchedule.Reminder): Pair<String, String> {
        val km = NumberFormat.getIntegerInstance(ptBr)
        val date = r.dueDateMillis?.let { SimpleDateFormat("dd/MM/yyyy", ptBr).format(Date(it)) }
        return when {
            r.overdue && r.track == ReminderSchedule.Track.TIME ->
                "${r.label}: prazo vencido" to "$vehicleName: o prazo terminou em $date. Toque para registrar o serviço."
            r.overdue ->
                "${r.label}: quilometragem vencida" to
                    "$vehicleName passou dos ${km.format(r.dueKm)} km previstos (está com ${km.format(currentKm)} km). Toque para registrar o serviço."
            r.track == ReminderSchedule.Track.TIME -> {
                val quando = when (r.left) {
                    1L -> "amanhã"
                    else -> "em ${r.left} dias"
                }
                "${r.label} $quando" to "$vehicleName: o prazo vence em $date. Toque para ver as manutenções do veículo."
            }
            else ->
                "Faltam ${km.format(r.left)} km: ${r.label}" to
                    "$vehicleName está com ${km.format(currentKm)} km; o serviço vence aos ${km.format(r.dueKm)} km."
        }
    }
}
