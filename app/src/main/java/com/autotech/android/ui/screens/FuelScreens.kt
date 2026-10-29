package com.autotech.android.ui.screens

import com.autotech.android.ui.components.shortName
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.ui.components.ReceiptAttachment
import com.autotech.android.util.PendingReceipt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.components.AtEventDates
import com.autotech.android.ui.components.dayIsBeforeOrEqual
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.parseDecimal
import com.autotech.android.util.ReceiptFileStore
import kotlinx.coroutines.launch
import com.autotech.android.data.mapper.toFillUp
import com.autotech.android.domain.FuelConsumption
import com.autotech.android.domain.FuelMix
import com.autotech.android.domain.FuelType
import com.autotech.android.domain.ConsumptionPeriod
import com.autotech.android.ui.components.AtCheckRow
import com.autotech.android.ui.components.AtStat
import com.autotech.android.ui.components.fuelType
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FuelListScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val recordsFlow = remember(vehicleId) { app.graph.fuelRepository.observeRecords(vehicleId) }
    val vehicleFlow = remember(vehicleId) { app.graph.vehicleRepository.observeVehicle(vehicleId) }
    val records by recordsFlow.collectAsState(initial = emptyList())
    val vehicle by vehicleFlow.collectAsState(initial = null)
    val df = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }

    AtScreen {
        AtHeader(
            "Abastecimentos",
            vehicle?.let { "${it.shortName} · ${it.plate}" },
            onBack = onBack
        ) {
            AtCircleButton(Icons.Rounded.Add, "Novo abastecimento", onAdd, dark = true)
        }

        Reveal(0) {
            val total = records.sumOf { it.amount ?: 0.0 }
            val liters = records.sumOf { it.liters }
            DarkSummaryCard(
                label = "Gasto com combustível",
                value = formatMoney(total),
                left = "${records.size} abastecimento(s)",
                right = String.format(PT_BR, "%.1f L no total", liters)
            )
        }

        // RF-05: consumo (km/L) dos abastecimentos em que o usuário pediu o cálculo
        val isFlex = vehicle?.fuelType == FuelType.FLEX
        val periods = remember(records) { FuelConsumption.periods(records.map { it.toFillUp() }) }
        val periodByFill = remember(periods) { periods.associateBy { it.fillId } }
        Reveal(1) {
            ConsumptionCard(periods, isFlex)
        }

        // RF-13: calculadora álcool x gasolina (regra dos 70%), só faz sentido em veículo flex
        // (ou enquanto o combustível não foi informado)
        if (vehicle?.fuelType == null || isFlex) {
            Reveal(1) {
                FuelCalculatorCard()
            }
        }

        Reveal(2) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Histórico", "Do mais recente ao mais antigo")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (records.isEmpty()) {
                        AtListRow(
                            title = "Nenhum abastecimento registrado",
                            subtitle = "Toque para registrar o primeiro",
                            icon = Icons.Rounded.LocalGasStation,
                            onClick = onAdd
                        )
                    }
                    records.forEach { r ->
                        val period = periodByFill[r.id]
                        AtListRow(
                            title = litersTitle(r),
                            subtitle = "${df.format(Date(r.dateMillis))} · ${formatKm(r.eventKm)} km" +
                                (period?.let { String.format(PT_BR, " · %.1f km/L", it.kmPerLiter) + burnedSuffix(it, isFlex) } ?: ""),
                            subtitleLines = 2,
                            icon = Icons.Rounded.LocalGasStation,
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    r.receiptPath?.let { path ->
                                        IconButton(onClick = { ReceiptFileStore.open(context, path) }) {
                                            Icon(Icons.Rounded.AttachFile, "Abrir comprovante", tint = AtColors.Primary)
                                        }
                                    }
                                    Text(r.amount?.let { formatMoney(it) } ?: "—", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        )
                    }
                }
            }
        }

        AtPillButton("Novo abastecimento", onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun AddFuelScreen(
    vehicleId: Long,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val scope = rememberCoroutineScope()
    var eventKm by remember { mutableStateOf("") }
    var vehicleKm by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    // RF-05: só o KM do dia do abastecimento é obrigatório; marcado como KM atual, ele atualiza o odômetro
    var isCurrent by remember { mutableStateOf(true) }
    // RF-05: litros de álcool e de gasolina (só flex, opcional) e cálculo de consumo (qualquer veículo)
    var specify by remember { mutableStateOf(false) }
    var ethanol by remember { mutableStateOf("") }
    var gasoline by remember { mutableStateOf("") }
    var compute by remember { mutableStateOf(false) }
    val fillsFlow = remember(vehicleId) { app.graph.fuelRepository.observeRecords(vehicleId) }
    val existing by fillsFlow.collectAsState(initial = emptyList())
    // comprovante escolhido (qualquer tipo de arquivo), guardado de vez ao salvar
    var pending by remember { mutableStateOf<PendingReceipt?>(null) }
    val vehicleFlow = remember(vehicleId) { app.graph.vehicleRepository.observeVehicle(vehicleId) }
    val vehicle by vehicleFlow.collectAsState(initial = null)
    LaunchedEffect(Unit) { ReceiptFileStore.cleanupPending(context) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    // RF-04 / RF-05: data do evento e data do registro (por padrão, o mesmo dia)
    var sameDay by remember { mutableStateOf(true) }
    var eventDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var registeredDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // por padrão, o KM do dia só é o KM atual quando o evento é de hoje
    LaunchedEffect(eventDate) {
        val a = java.util.Calendar.getInstance().apply { timeInMillis = eventDate }
        val b = java.util.Calendar.getInstance()
        isCurrent = a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    }


    val isFlex = vehicle?.fuelType == FuelType.FLEX
    val useSplit = isFlex && specify
    fun totalLiters(): Double? =
        if (useSplit) {
            val e = parseDecimal(ethanol) ?: 0.0
            val g = parseDecimal(gasoline) ?: 0.0
            (e + g).takeIf { it > 0 }
        } else parseDecimal(liters)

    fun save() {
        if (!dayIsBeforeOrEqual(eventDate, registeredDate)) {
            error = "A data do abastecimento não pode ser depois da data do registro."
            return
        }
        val ek = eventKm.toLongOrNull() ?: run {
            error = "Informe o KM no dia do abastecimento."
            return
        }
        // RN-02 / RN-05: o KM atual é opcional; sem ele, o odômetro do veículo não muda
        val typedCurrent: Long? = if (isCurrent) ek else vehicleKm.toLongOrNull()
        val vk = typedCurrent ?: maxOf(vehicle?.currentOdometerKm ?: ek, ek)
        val lit = totalLiters()?.takeIf { it > 0 } ?: run {
            error = if (useSplit) "Informe os litros de álcool e/ou de gasolina." else "Litros inválidos"
            return
        }
        val am = parseDecimal(amount)
        scope.launch {
            saving = true
            error = null
            val base = ReceiptFileStore.baseName("Abastecimento", vehicle?.shortName, vehicle?.plate, null, eventDate)
            val receiptPath = pending?.let { ReceiptFileStore.finalize(context, it, base) }
            val record = FuelRecordEntity(
                vehicleId = vehicleId,
                dateMillis = eventDate,
                registeredAtMillis = registeredDate,
                eventKm = ek,
                vehicleKm = vk,
                liters = lit,
                amount = am,
                receiptPath = receiptPath,
                ethanolLiters = if (useSplit) parseDecimal(ethanol)?.takeIf { it > 0 } else null,
                gasolineLiters = if (useSplit) parseDecimal(gasoline)?.takeIf { it > 0 } else null,
                computeConsumption = compute
            )
            app.graph.fuelRepository.addRecord(record, updateVehicleOdometerTo = typedCurrent)
                .onSuccess { onDone() }
                .onFailure {
                    receiptPath?.let { path -> pending?.let { pr -> ReceiptFileStore.revert(path, pr) } }
                    error = it.message
                }
            saving = false
        }
    }

    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AtScreen {
        AtHeader("Novo abastecimento", "Registro de combustível", onBack = onCancel)

        Reveal(0) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Datas", "Quando abasteceu e quando está sendo registrado")
                Spacer(Modifier.height(10.dp))
                AtEventDates(
                    eventLabel = "Abastecimento",
                    sameDay = sameDay,
                    onSameDayChange = { same ->
                        sameDay = same
                        registeredDate = if (same) eventDate else System.currentTimeMillis()
                    },
                    eventMillis = eventDate,
                    onEventChange = { eventDate = it },
                    registeredMillis = registeredDate,
                    onRegisteredChange = { registeredDate = it }
                )
            }
        }

        Reveal(0) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Quilometragem", "Odômetro registrado: ${vehicle?.let { formatKm(it.currentOdometerKm) + " km" } ?: "\u2014"}")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtField(eventKm, { eventKm = it.filter { c -> c.isDigit() } }, "KM no dia do abastecimento", leadingIcon = Icons.Rounded.Speed, keyboardOptions = numeric)
                    AtCheckRow("Este é o KM atual do veículo", isCurrent) { isCurrent = it }
                    if (!isCurrent) {
                        AtField(vehicleKm, { vehicleKm = it.filter { c -> c.isDigit() } }, "KM atual do veículo (opcional)", leadingIcon = Icons.Rounded.Speed, keyboardOptions = numeric)
                        Text(
                            "Se não souber o KM atual, deixe em branco: o odômetro do veículo continua como está.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtColors.Muted
                        )
                    }
                }
            }
        }

        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Abastecimento")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isFlex) {
                        AtCheckRow("Especificar litros de álcool e de gasolina", specify) { specify = it }
                    }
                    if (useSplit) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AtField(ethanol, { ethanol = it }, "Litros de álcool", leadingIcon = Icons.Rounded.WaterDrop, keyboardOptions = decimal, modifier = Modifier.weight(1f))
                            AtField(gasoline, { gasoline = it }, "Litros de gasolina", leadingIcon = Icons.Rounded.WaterDrop, keyboardOptions = decimal, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                String.format(PT_BR, "Total: %.2f L", totalLiters() ?: 0.0),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            AtField(amount, { amount = it }, "Valor (R$)", keyboardOptions = decimal, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AtField(liters, { liters = it }, "Litros", leadingIcon = Icons.Rounded.WaterDrop, keyboardOptions = decimal, modifier = Modifier.weight(1f))
                            AtField(amount, { amount = it }, "Valor (R$)", keyboardOptions = decimal, modifier = Modifier.weight(1f))
                        }
                    }
                    AtCheckRow("Calcular consumo (km/L)", compute) { compute = it }
                    if (compute) {
                        ConsumptionPreview(
                            fills = existing.map { it.toFillUp() },
                            km = eventKm.toLongOrNull(),
                            liters = totalLiters(),
                            showFuel = isFlex
                        )
                    }
                    ReceiptAttachment(
                        pending = pending,
                        onChange = { pending = it },
                        finalBaseName = ReceiptFileStore.baseName("Abastecimento", vehicle?.shortName, vehicle?.plate, null, eventDate)
                    )
                }
            }
        }

        error?.let { AtMessage(it, isError = true) }

        AtPillButton("Salvar abastecimento", { save() }, Modifier.fillMaxWidth(), loading = saving)
        AtPillButton("Cancelar", onCancel, Modifier.fillMaxWidth(), style = AtButtonStyle.Light)
        Spacer(Modifier.height(8.dp))
    }
}

// ─── RF-05: consumo ─────────────────────────────────────────────────────────────

private fun litersTitle(r: FuelRecordEntity): String {
    val e = r.ethanolLiters
    val g = r.gasolineLiters
    return when {
        e != null && g != null -> String.format(PT_BR, "%.2f L (%.1f álcool + %.1f gasolina)", r.liters, e, g)
        e != null -> String.format(PT_BR, "%.2f L de álcool", r.liters)
        g != null -> String.format(PT_BR, "%.2f L de gasolina", r.liters)
        else -> String.format(PT_BR, "%.2f L", r.liters)
    }
}

private fun burnedSuffix(p: ConsumptionPeriod, isFlex: Boolean): String =
    if (!isFlex) "" else when (p.burned) {
        FuelMix.ETHANOL -> " com álcool"
        FuelMix.GASOLINE -> " com gasolina"
        FuelMix.MIXED -> " com mistura"
        FuelMix.UNKNOWN -> ""
    }

/** Médias de consumo do veículo; em flex, também por combustível. */
@Composable
private fun ConsumptionCard(periods: List<ConsumptionPeriod>, isFlex: Boolean) {
    val s = FuelConsumption.summarize(periods)
    AtCard(Modifier.fillMaxWidth()) {
        if (s.overall == null) {
            AtCardTitle("Consumo", "Marque \u201cCalcular consumo\u201d ao abastecer com o tanque cheio")
            return@AtCard
        }
        AtCardTitle("Consumo", "Calculado em ${s.periods} ${if (s.periods == 1) "abastecimento" else "abastecimentos"}")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            AtStat("Média", String.format(PT_BR, "%.1f km/L", s.overall), Modifier.weight(1f))
            if (isFlex) {
                AtStat("Com álcool", s.ethanol?.let { String.format(PT_BR, "%.1f km/L", it) } ?: "\u2014", Modifier.weight(1f))
                AtStat("Com gasolina", s.gasoline?.let { String.format(PT_BR, "%.1f km/L", it) } ?: "\u2014", Modifier.weight(1f))
            }
        }
    }
}

/** Prévia do consumo no formulário, antes de salvar. */
@Composable
private fun ConsumptionPreview(fills: List<com.autotech.android.domain.FillUp>, km: Long?, liters: Double?, showFuel: Boolean) {
    val df = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }
    val previous = km?.let { FuelConsumption.previousOf(fills, it) }
    val text = when {
        fills.isEmpty() ->
            "Este é o primeiro abastecimento do veículo. Encha o tanque: o consumo será calculado no próximo abastecimento com esta opção marcada."
        km == null -> "Informe o KM do evento para ver o consumo."
        previous == null -> "Não há abastecimento anterior a este KM, então o consumo não pode ser calculado."
        else -> {
            val driven = km - previous.km
            val fuel = if (!showFuel) "" else when (FuelConsumption.mixOf(previous.ethanolLiters, previous.gasolineLiters)) {
                FuelMix.ETHANOL -> " com álcool"
                FuelMix.GASOLINE -> " com gasolina"
                FuelMix.MIXED -> " com mistura de álcool e gasolina"
                FuelMix.UNKNOWN -> ""
            }
            val base = "Rodou ${formatKm(driven)} km$fuel desde o abastecimento de ${df.format(Date(previous.dateMillis))} (${formatKm(previous.km)} km)"
            if (liters == null || liters <= 0) "$base. Informe os litros para ver o consumo."
            else String.format(PT_BR, "%s: %.1f km/L.", base, driven / liters)
        }
    }
    AtMessage(text, isError = false)
    Text(
        "Para o cálculo ficar certo, encha o tanque neste abastecimento e no anterior.",
        style = MaterialTheme.typography.bodySmall,
        color = AtColors.Muted
    )
}
