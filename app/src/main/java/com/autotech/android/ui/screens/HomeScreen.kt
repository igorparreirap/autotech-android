package com.autotech.android.ui.screens

import com.autotech.android.domain.MaintenanceCycles
import com.autotech.android.data.mapper.toCycleInputs
import com.autotech.android.ui.components.describe
import com.autotech.android.ui.components.shortName
import com.autotech.android.ui.components.displayName
import com.autotech.android.ui.components.fuelLabel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.MaintenanceCatalog
import com.autotech.android.ui.components.AtAvatar
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtCarStage
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.components.AtDecorMap
import com.autotech.android.ui.components.AtGauge
import com.autotech.android.ui.components.AtLabel
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtLogo
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtPlate
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.AtSegmented
import com.autotech.android.ui.components.AtTileGrid
import com.autotech.android.ui.components.AtTrack
import com.autotech.android.ui.components.AtWaveform
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.TileSpec
import com.autotech.android.ui.components.VehiclePhotoStage
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.maintenanceIcon
import com.autotech.android.ui.theme.AtColors
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.max

@Composable
fun HomeScreen(
    userId: Long,
    userName: String,
    onVehicles: () -> Unit,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onMaintenance: (Long) -> Unit,
    onFuel: (Long) -> Unit,
    onFuelAdd: (Long) -> Unit,
    onDashboard: () -> Unit,
    onMap: () -> Unit,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
    onOpenPdf: (String) -> Unit,
    onCompletePending: (vehicleId: Long, pendingId: Long) -> Unit,
    onSignOut: () -> Unit
) {
    var showPdf by remember { mutableStateOf(false) }
    val app = LocalContext.current.applicationContext as AutoTechApplication
    val vehiclesFlow = remember(userId) { app.graph.vehicleRepository.observeVehicles(userId) }
    val vehicles by vehiclesFlow.collectAsState(initial = null)
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val list = vehicles.orEmpty()
    val vehicle = list.getOrNull(selected.coerceIn(0, (list.size - 1).coerceAtLeast(0)))
    val vid = vehicle?.id

    val recordsFlow = remember(vid) {
        if (vid != null) app.graph.maintenanceRepository.observeRecords(vid) else flowOf(emptyList<MaintenanceRecordEntity>())
    }
    val cyclesFlow = remember(vid) {
        if (vid != null) app.graph.maintenanceRepository.observeCycles(vid) else flowOf(emptyList<MaintenanceCycleEntity>())
    }
    val fuelFlow = remember(vid) {
        if (vid != null) app.graph.fuelRepository.observeRecords(vid) else flowOf(emptyList<FuelRecordEntity>())
    }
    val records by recordsFlow.collectAsState(initial = emptyList())
    val cycles by cyclesFlow.collectAsState(initial = emptyList())
    val fuels by fuelFlow.collectAsState(initial = emptyList())

    // RN-01 / RN-04: cálculo no domínio (domain/MaintenanceCycles)
    // RF-06: itens marcados como "Já fiz" e ainda sem os dados (de todos os veículos)
    val pendingFlow = remember(userId) { app.graph.maintenanceRepository.observePendingByUser(userId) }
    val pendingAll by pendingFlow.collectAsState(initial = emptyList())
    val statuses = remember(vehicle, cycles, records, pendingAll) {
        val skip = pendingAll.filter { it.vehicleId == vehicle?.id }.map { it.itemKey }.toSet()
        vehicle?.let { MaintenanceCycles.evaluate(it.currentOdometerKm, toCycleInputs(cycles, records)) }.orEmpty()
            .filter { it.itemKey !in skip }
    }

    // Gastos do mês corrente (veículo selecionado)
    val monthKey = remember { SimpleDateFormat("yyyy-MM", PT_BR).format(Date()) }
    val monthName = remember { SimpleDateFormat("MMMM", PT_BR).format(Date()) }
    val keyFmt = remember { SimpleDateFormat("yyyy-MM", PT_BR) }
    val monthMaintenance = records.filter { keyFmt.format(Date(it.eventDateMillis)) == monthKey }.sumOf { it.amount ?: 0.0 }
    val monthFuel = fuels.filter { keyFmt.format(Date(it.dateMillis)) == monthKey }.sumOf { it.amount ?: 0.0 }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }

    // RF-06: pede a permissão de notificações (Android 13+) uma vez, ao abrir a tela inicial
    val homeContext = LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) com.autotech.android.work.AlertScheduler.checkNow(homeContext) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!askedNotifications && android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(homeContext, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AtScreen {
        // ── Topo: logo + busca + avatar ─────────────────────────────────────
        Reveal(0) {
            TopBar(
                userName = userName,
                onSearch = onSearch,
                onBell = onNotifications,
                hasAlert = statuses.any { it.soon } || pendingAll.isNotEmpty(),
                onSignOut = onSignOut
            )
        }

        Reveal(1) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text(
                    "$greeting, ${userName.substringBefore(' ')}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(Date())
                        .replaceFirstChar { it.titlecase(PT_BR) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtColors.Muted
                )
            }
        }

        if (list.size > 1) {
            Reveal(2) {
                AtSegmented(
                    options = list.map { it.shortName.take(12) },
                    selectedIndex = selected.coerceIn(0, list.size - 1),
                    onSelect = { selected = it }
                )
            }
        }

        // ── Veículo em destaque ("Car Details") ─────────────────────────────
        Reveal(3) {
            when {
                vehicles == null -> AtCard(Modifier.fillMaxWidth().height(320.dp)) {}
                vehicle == null -> EmptyVehicleCard(onAddVehicle)
                else -> VehicleHeroCard(vehicle, onOpen = { onOpenVehicle(vehicle.id) })
            }
        }

        // ── Abastecer: ação mais frequente, logo abaixo do veículo ─────────
        if (vehicle != null) {
            Reveal(3) {
                AtPillButton(
                    "Abastecer",
                    { onFuelAdd(vehicle.id) },
                    Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.LocalGasStation
                )
            }
        }

        // ── Pendentes: marcadas como "Já fiz", falta completar os dados ─────
        if (pendingAll.isNotEmpty()) {
            Reveal(3) {
                val pdf = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Manutenções pendentes", "Você marcou como feitas: toque para completar")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingAll.forEach { p ->
                            val owner = list.firstOrNull { it.id == p.vehicleId }
                            AtListRow(
                                title = MaintenanceCatalog.label(p.itemKey),
                                subtitle = listOfNotNull(owner?.shortName, "feita em ${pdf.format(Date(p.doneDateMillis))}").joinToString(" · ") +
                                    "\nFalta: km, valor, descrição e comprovante",
                                subtitleLines = 2,
                                icon = maintenanceIcon(p.itemKey),
                                iconBg = AtColors.AccentSoft,
                                iconTint = AtColors.Primary,
                                onClick = { onCompletePending(p.vehicleId, p.id) },
                                trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted) }
                            )
                        }
                    }
                }
            }
        }

        if (vehicle != null) {
            val main = statuses.firstOrNull()
            // ── Odômetro ("Climate") + Próxima revisão ("Fuel Level") ───────
            Reveal(4) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AtCard(
                        Modifier
                            .weight(1f)
                            .height(214.dp),
                        onClick = { onOpenVehicle(vehicle.id) }
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Text("Odômetro", style = MaterialTheme.typography.titleMedium)
                            AtLabel(vehicle.fuelLabel?.let { "Atual · $it" } ?: "Quilometragem atual")
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatKm(vehicle.currentOdometerKm),
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("km", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Rounded.Speed, null, Modifier.size(26.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            val kmCycle = statuses.firstOrNull { it.endKm != null }
                            AtLabel(
                                if (kmCycle?.kmLeft != null) "Próx. ${kmCycle.label}" else "Sem ciclo por km",
                                color = AtColors.Ink
                            )
                            Spacer(Modifier.weight(1f))
                            if (kmCycle?.endKm != null) {
                                Row {
                                    AtLabel(formatKm(kmCycle.startKm))
                                    Spacer(Modifier.weight(1f))
                                    AtLabel(formatKm(kmCycle.endKm))
                                }
                                AtTrack(kmCycle.usedFraction)
                            } else {
                                AtTrack(0.5f)
                            }
                        }
                    }

                    AtCard(
                        Modifier
                            .weight(1f)
                            .height(214.dp),
                        onClick = { onMaintenance(vehicle.id) }
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Revisão", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.MoreHoriz, null, Modifier.size(20.dp))
                            }
                            AtLabel("Vida útil restante")
                            Spacer(Modifier.weight(1f))
                            val remaining = main?.let { (1f - it.usedFraction).coerceIn(0f, 1f) }
                            AtGauge(
                                fraction = remaining ?: 0f,
                                centerText = remaining?.let { "${(it * 100).toInt()}%" } ?: "—",
                                progressColor = if (main?.overdue == true) AtColors.Danger else AtColors.Primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.weight(1f))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(AtColors.Field)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (main?.label ?: "Sem ciclo").uppercase(PT_BR),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(
                                            when {
                                                main == null -> AtColors.Faint
                                                main.overdue -> AtColors.Danger
                                                else -> AtColors.Accent
                                            },
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Lembretes ("AI Assistant") ──────────────────────────────────────
        if (vehicle != null) {
            Reveal(6) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Lembretes", "Ciclos de manutenção do ${vehicle.shortName}")
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (statuses.isEmpty()) {
                            AtListRow(
                                title = "Nenhum ciclo definido",
                                subtitle = "Defina km/meses ao registrar uma manutenção",
                                icon = Icons.Rounded.NotificationsNone,
                                onClick = { onMaintenance(vehicle.id) },
                                trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted) }
                            )
                        } else {
                            statuses.take(4).forEach { s ->
                                AtListRow(
                                    title = s.label,
                                    subtitle = s.describe(),
                                    icon = maintenanceIcon(s.itemKey),
                                    onClick = { onMaintenance(vehicle.id) },
                                    iconBg = if (s.overdue) AtColors.DangerSoft else AtColors.Card,
                                    iconTint = if (s.overdue) AtColors.Danger else AtColors.Ink,
                                    trailing = {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .background(if (s.overdue) AtColors.Danger else AtColors.Accent, CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Gastos do mês ("player" verde) ──────────────────────────────────
        if (vehicle != null) {
            Reveal(8) {
                val total = monthMaintenance + monthFuel
                AtCard(Modifier.fillMaxWidth(), onClick = onDashboard) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(AtColors.Primary, AtColors.Deep)))
                            .padding(16.dp)
                    ) {
                        Text(
                            "GASTOS\n${monthName.uppercase(PT_BR)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, lineHeight = 22.sp),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                        Text(
                            "// ${vehicle.plate}\n// manutenção\n// combustível",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        AnimatedContent(
                            targetState = total,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.align(Alignment.BottomStart),
                            label = "total"
                        ) { t ->
                            Text(
                                formatMoney(t),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        vehicle.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Gastos do mês",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoney(monthMaintenance), style = MaterialTheme.typography.labelSmall, color = AtColors.Muted)
                        AtWaveform(
                            fraction = if (total > 0) (monthMaintenance / total).toFloat() else 0f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(formatMoney(monthFuel), style = MaterialTheme.typography.labelSmall, color = AtColors.Muted)
                    }
                    Row {
                        Text("Manutenção", style = MaterialTheme.typography.labelSmall, color = AtColors.Muted)
                        Spacer(Modifier.weight(1f))
                        Text("Combustível", style = MaterialTheme.typography.labelSmall, color = AtColors.Muted)
                    }
                }
            }
        }

        // ── Localização real (Google Maps + Places) junto dos atalhos ───────
        Reveal(9) {
            HomeLocationCard(onOpenFullMap = onMap)
        }

        Reveal(10) {
            val v = vehicle
            AtTileGrid(
                listOf(
                    TileSpec("Manutenção", Icons.Rounded.Build) { if (v != null) onMaintenance(v.id) else onAddVehicle() },
                    TileSpec("Combustível", Icons.Rounded.LocalGasStation) { if (v != null) onFuel(v.id) else onAddVehicle() },
                    TileSpec("Veículos", Icons.Rounded.DirectionsCar, onVehicles),
                    TileSpec("Serviços", Icons.Rounded.Map, onMap),
                    TileSpec("Dashboard", Icons.Rounded.Insights, onDashboard),
                    TileSpec("Relatório PDF", Icons.Rounded.PictureAsPdf) { if (v != null) showPdf = true else onAddVehicle() }
                )
            )
        }

        if (showPdf && vehicle != null) {
            VehicleReportDialog(
                vehicle = vehicle,
                onDismiss = { showPdf = false },
                onGenerated = { file -> showPdf = false; onOpenPdf(file.absolutePath) }
            )
        }

        // ── Sair ("Emergency") ──────────────────────────────────────────────
        Reveal(11) {
            AtCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(AtColors.DangerSoft, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = AtColors.Danger, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Sair da conta", style = MaterialTheme.typography.titleSmall)
                        Text("Seus dados continuam salvos no aparelho", style = MaterialTheme.typography.bodySmall, color = AtColors.Muted)
                    }
                    AtPillButton("Sair", onSignOut, compact = true)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar(
    userName: String,
    onSearch: () -> Unit,
    onBell: () -> Unit,
    hasAlert: Boolean,
    onSignOut: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AtLogo(38.dp)
        Surface(
            onClick = onSearch,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = CircleShape,
            color = AtColors.Card
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = AtColors.Faint)
                Spacer(Modifier.width(8.dp))
                Text("Buscar no app...", color = AtColors.Faint, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
        AtCircleButton(Icons.Rounded.NotificationsNone, "Alertas", onBell, dot = hasAlert)
        Box {
            Row(
                Modifier
                    .clip(CircleShape)
                    .clickable { menu = true }
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtAvatar(userName, 40.dp)
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(userName, style = MaterialTheme.typography.titleSmall) },
                    onClick = { menu = false },
                    enabled = false
                )
                DropdownMenuItem(
                    text = { Text("Sair") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) },
                    onClick = { menu = false; onSignOut() }
                )
            }
        }
    }
}

@Composable
private fun VehicleHeroCard(vehicle: VehicleEntity, onOpen: () -> Unit) {
    AtCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Meu veículo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(AtColors.Field)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                // Sem combustível cadastrado, o selo convida a informar (antes mostrava "Ativo").
                val fuelLabel = vehicle.fuelLabel
                Text(
                    fuelLabel ?: "Informar combustível",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fuelLabel == null) AtColors.Primary else AtColors.Muted
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.MoreHoriz, null, Modifier.size(22.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            vehicle.displayName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )
        val sub = listOfNotNull(
            listOfNotNull(vehicle.year, vehicle.modelYear).distinct().joinToString("/").ifEmpty { null },
            vehicle.version,
            vehicle.color
        ).joinToString(" · ")
        if (sub.isNotEmpty()) {
            Text(
                sub,
                style = MaterialTheme.typography.bodyMedium,
                color = AtColors.Faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Foto enviada pelo usuário no lugar do carro padrão (botão de câmera no canto)
        VehiclePhotoStage(vehicle.id, Modifier.padding(vertical = 10.dp), height = 190.dp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AtPlate(vehicle.plate)
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun EmptyVehicleCard(onAdd: () -> Unit) {
    AtCard(Modifier.fillMaxWidth()) {
        Text("Meu veículo", style = MaterialTheme.typography.titleMedium)
        AtCarStage(iconSize = 150.dp, icon = Icons.Rounded.DirectionsCar)
        Text(
            "Nenhum veículo ainda",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Cadastre pela placa e acompanhe revisões por tempo e quilometragem.",
            style = MaterialTheme.typography.bodyMedium,
            color = AtColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        AtPillButton(
            "Adicionar veículo", onAdd,
            Modifier.fillMaxWidth(),
            style = AtButtonStyle.Dark,
            icon = Icons.Rounded.Add
        )
    }
}
