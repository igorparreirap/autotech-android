package com.autotech.android.ui.navigation

import androidx.compose.runtime.mutableStateOf

/** Veículo cujo histórico de manutenções deve abrir ao tocar numa notificação (RF-06). */
object PendingOpen {
    val maintenanceVehicleId = mutableStateOf<Long?>(null)
}
