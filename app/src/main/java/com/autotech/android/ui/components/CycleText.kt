package com.autotech.android.ui.components

import com.autotech.android.domain.CycleStatus
import kotlin.math.abs

/** Texto do ciclo para a interface: "Faltam 1.200 km · 30 dias". */
fun CycleStatus.describe(): String {
    val parts = mutableListOf<String>()
    kmLeft?.let { parts += if (it >= 0) "Faltam ${formatKm(it)} km" else "Vencido há ${formatKm(abs(it))} km" }
    daysLeft?.let { parts += if (it >= 0) "$it dias" else "vencido há ${abs(it)} dias" }
    return parts.joinToString(" · ")
}
