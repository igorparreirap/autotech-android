package com.autotech.android.work

import android.content.Context

/**
 * RN-03: registro da última etapa avisada de cada ciclo (veículo + critério + ciclo),
 * para que nenhuma etapa seja avisada duas vezes.
 */
class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("autotech_reminders", Context.MODE_PRIVATE)

    fun lastStage(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    fun save(key: String, stage: Long, at: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(key, stage).putLong("$key@quando", at).apply()
    }

    /** "Lembrar amanhã": o aviso volta depois de [until]. */
    fun snooze(key: String, until: Long) {
        prefs.edit().putLong("$key@adiar", until).apply()
    }

    fun snoozeDue(key: String, now: Long): Boolean {
        val until = prefs.getLong("$key@adiar", 0L)
        return until in 1..now
    }

    fun clearSnooze(key: String) {
        prefs.edit().remove("$key@adiar").apply()
    }
}
