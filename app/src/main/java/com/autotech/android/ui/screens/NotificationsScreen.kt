package com.autotech.android.ui.screens

import com.autotech.android.domain.CycleStatus
import com.autotech.android.domain.MaintenanceCycles
import com.autotech.android.data.mapper.toCycleInputs
import com.autotech.android.ui.components.describe
import com.autotech.android.ui.components.shortName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.autotech.android.work.ReminderNotifier
import com.autotech.android.ui.components.AtPillButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.maintenanceIcon
import com.autotech.android.ui.theme.AtColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

/** A partir de 80% do intervalo consumido o ciclo entra como "próximo". */
private const val SOON_FRACTION = 0.8f

private data class AlertItem(val vehicle: VehicleEntity, val status: CycleStatus)

/**
 * Notificações recentes: avisos de manutenção vencida ou próxima de todos os veículos,
 * calculados pelos ciclos definidos pelo usuário (RF06 / RF11 / RN04).
 */
@Composable
fun NotificationsScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenMaintenance: (Long) -> Unit,
    onCompletePending: (vehicleId: Long, pendingId: Long) -> Unit = { _, _ -> }
) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // RF-06: itens marcados como "Já fiz" saem dos avisos e vão para "Pendentes"
    val pendingFlow = remember(userId) { app.graph.maintenanceRepository.observePendingByUser(userId) }
    val pendingAll by pendingFlow.collectAsState(initial = emptyList())
    var vehicles by remember { mutableStateOf<List<VehicleEntity>>(emptyList()) }

    val alerts by produceState<List<AlertItem>?>(initialValue = null, userId, pendingAll) {
        val items = mutableListOf<AlertItem>()
        val found = app.graph.vehicleRepository.listVehicles(userId)
        vehicles = found
        for (v in found) {
            val cycles = app.graph.maintenanceRepository.observeCycles(v.id).first()
            val records = app.graph.maintenanceRepository.listForPdf(v.id)
            val skip = pendingAll.filter { it.vehicleId == v.id }.map { it.itemKey }.toSet()
            MaintenanceCycles.evaluate(v.currentOdometerKm, toCycleInputs(cycles, records))
                .filter { it.usedFraction >= SOON_FRACTION && it.itemKey !in skip }
                .forEach { items += AlertItem(v, it) }
        }
        value = items.sortedByDescending { it.status.usedFraction }
    }
    val markDone: (Long, String) -> Unit = { vehicleId, itemKey ->
        scope.launch { app.graph.maintenanceRepository.markDone(vehicleId, itemKey, System.currentTimeMillis()) }
    }

    // RF-06: estado das notificações do sistema, conferido de novo quando o usuário volta das configurações
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(ReminderNotifier.canPost(context)) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) notificationsOn = ReminderNotifier.canPost(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AtScreen {
        AtHeader("Notificações recentes", "Lembretes de manutenção", onBack = onBack)

        if (!notificationsOn) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Notificações desativadas", "Sem elas, os avisos só aparecem aqui dentro do app")
                Spacer(Modifier.height(12.dp))
                AtPillButton(
                    "Ativar notificações",
                    {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    Modifier.fillMaxWidth()
                )
            }
        }

        AtCard(Modifier.fillMaxWidth()) {
            AtCardTitle("Como funcionam os avisos", "Cada aviso chega uma única vez por ciclo")
            Spacer(Modifier.height(10.dp))
            Text(
                "Por tempo: 30, 15 e 7 dias antes do vencimento em um ciclo de 12 meses; ciclos menores recebem prazos proporcionais.\n" +
                    "Por quilometragem: quando faltar metade, 10% e 5% do intervalo (ex.: 5.000, 1.000 e 500 km em um ciclo de 10.000 km).\n" +
                    "E um último aviso quando a manutenção vencer.",
                style = MaterialTheme.typography.bodySmall,
                color = AtColors.Muted
            )
        }

        if (pendingAll.isNotEmpty()) {
            val pdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")) }
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Pendentes", "Marcadas como feitas: toque para completar os dados")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendingAll.forEach { p ->
                        val owner = vehicles.firstOrNull { it.id == p.vehicleId }
                        AtListRow(
                            title = com.autotech.android.domain.MaintenanceCatalog.label(p.itemKey),
                            subtitle = listOfNotNull(owner?.shortName, "feita em ${pdf.format(java.util.Date(p.doneDateMillis))}").joinToString(" · "),
                            icon = maintenanceIcon(p.itemKey),
                            iconBg = AtColors.AccentSoft,
                            iconTint = AtColors.Primary,
                            onClick = { onCompletePending(p.vehicleId, p.id) }
                        )
                    }
                }
            }
        }

        val list = alerts
        when {
            list == null -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = AtColors.Ink) }

            list.isEmpty() -> Reveal(0) {
                AtCard(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(56.dp)
                            .background(AtColors.Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.TaskAlt, null, tint = AtColors.Ink)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Tudo em dia",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Nenhuma manutenção vencida ou próxima. Os avisos aparecem aqui quando um ciclo de km ou meses estiver perto do fim.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> {
                val overdue = list.filter { it.status.overdue }
                val soon = list.filterNot { it.status.overdue }
                if (overdue.isNotEmpty()) {
                    Reveal(0) { AlertSection("Vencidas", "Faça o serviço o quanto antes", overdue, onOpenMaintenance, markDone) }
                }
                if (soon.isNotEmpty()) {
                    Reveal(1) { AlertSection("Próximas", "Chegando no fim do intervalo", soon, onOpenMaintenance, markDone) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AlertSection(
    title: String,
    subtitle: String,
    items: List<AlertItem>,
    onOpenMaintenance: (Long) -> Unit,
    onMarkDone: (Long, String) -> Unit
) {
    AtCard(Modifier.fillMaxWidth()) {
        AtCardTitle(title, subtitle) {
            Box(
                Modifier
                    .background(if (items.first().status.overdue) AtColors.DangerSoft else AtColors.AccentSoft, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("${items.size}", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (vehicle, s) ->
                AtListRow(
                    title = if (s.overdue) "${s.label} vencida" else "${s.label} se aproximando",
                    subtitle = "${vehicle.shortName} · ${vehicle.plate}\n${s.describe()}",
                    subtitleLines = 2,
                    icon = maintenanceIcon(s.itemKey),
                    iconBg = if (s.overdue) AtColors.DangerSoft else AtColors.Card,
                    iconTint = if (s.overdue) AtColors.Danger else AtColors.Ink,
                    onClick = { onOpenMaintenance(vehicle.id) },
                    trailing = {
                        AtPillButton(
                            "Já fiz",
                            { onMarkDone(vehicle.id, s.itemKey) },
                            style = com.autotech.android.ui.components.AtButtonStyle.Accent,
                            compact = true
                        )
                    }
                )
            }
        }
    }
}
