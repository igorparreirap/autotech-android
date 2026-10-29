package com.autotech.android.ui.screens

import com.autotech.android.ui.components.shortName
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import com.autotech.android.ui.components.AtCheckRow
import com.autotech.android.ui.components.ReceiptAttachment
import com.autotech.android.util.PendingReceipt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.domain.MaintenanceCatalog
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtChip
import com.autotech.android.ui.components.AtEventDates
import com.autotech.android.ui.components.dayIsBeforeOrEqual
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtLabel
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.maintenanceIcon
import com.autotech.android.ui.components.parseDecimal
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.ReceiptFileStore
import kotlinx.coroutines.launch
import com.autotech.android.domain.MaintenanceCost
import com.autotech.android.ui.components.MaintenanceItemPicker
import com.autotech.android.ui.components.fuelType
import com.autotech.android.data.mapper.toCycleInputs
import com.autotech.android.domain.MaintenanceCycles
import com.autotech.android.ui.components.describe
import java.text.SimpleDateFormat
import java.util.Date

/** Cartão-resumo escuro (como a caixa preta sobre o mapa do layout). */
@Composable
internal fun DarkSummaryCard(label: String, value: String, left: String, right: String) {
    AtCard(Modifier.fillMaxWidth(), color = AtColors.Deep, contentColor = Color.White) {
        AtLabel(label, color = Color(0xFF8E8E8E))
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Row {
            Text(left, style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDBDBD))
            Spacer(Modifier.weight(1f))
            Text(right, style = MaterialTheme.typography.labelMedium, color = AtColors.Accent)
        }
    }
}

@Composable
fun MaintenanceListScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onCompletePending: (Long) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val recordsFlow = remember(vehicleId) { app.graph.maintenanceRepository.observeRecords(vehicleId) }
    val vehicleFlow = remember(vehicleId) { app.graph.vehicleRepository.observeVehicle(vehicleId) }
    val records by recordsFlow.collectAsState(initial = emptyList())
    val vehicle by vehicleFlow.collectAsState(initial = null)
    val df = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }
    // RF-06: ciclos em andamento e itens marcados como "Já fiz"
    val cyclesFlow = remember(vehicleId) { app.graph.maintenanceRepository.observeCycles(vehicleId) }
    val cycles by cyclesFlow.collectAsState(initial = emptyList())
    val pendingFlow = remember(vehicleId) { app.graph.maintenanceRepository.observePending(vehicleId) }
    val pendingList by pendingFlow.collectAsState(initial = emptyList())
    val statuses = remember(vehicle, cycles, records, pendingList) {
        val skip = pendingList.map { it.itemKey }.toSet()
        vehicle?.let { MaintenanceCycles.evaluate(it.currentOdometerKm, toCycleInputs(cycles, records)) }.orEmpty()
            .filter { it.itemKey !in skip }
    }

    AtScreen {
        AtHeader(
            "Manutenções",
            vehicle?.let { "${it.shortName} · ${it.plate}" },
            onBack = onBack
        ) {
            AtCircleButton(Icons.Rounded.Add, "Nova manutenção", onAdd, dark = true)
        }

        Reveal(0) {
            val total = records.sumOf { it.amount ?: 0.0 }
            DarkSummaryCard(
                label = "Total investido",
                value = formatMoney(total),
                left = "${records.size} registro(s)",
                right = records.firstOrNull()?.let { "Último: ${df.format(Date(it.eventDateMillis))}" } ?: "Sem registros"
            )
        }

        if (pendingList.isNotEmpty()) {
            Reveal(1) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Pendentes", "Você marcou como feitas: complete os dados")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pendingList.forEach { p ->
                            AtListRow(
                                title = MaintenanceCatalog.label(p.itemKey),
                                subtitle = "Feita em ${df.format(Date(p.doneDateMillis))}\nFalta: km, valor, descrição e comprovante",
                                subtitleLines = 2,
                                icon = maintenanceIcon(p.itemKey),
                                iconBg = AtColors.AccentSoft,
                                iconTint = AtColors.Primary,
                                onClick = { onCompletePending(p.id) },
                                trailing = {
                                    AtPillButton(
                                        "Desfazer",
                                        { scope.launch { app.graph.maintenanceRepository.undoPending(p.id) } },
                                        style = AtButtonStyle.Light,
                                        compact = true
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (statuses.isNotEmpty()) {
            Reveal(1) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Próximas trocas", "Já fez e não registrou? Toque em \u201cJá fiz\u201d")
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        statuses.forEach { s ->
                            AtListRow(
                                title = s.label,
                                subtitle = s.describe(),
                                subtitleLines = 2,
                                icon = maintenanceIcon(s.itemKey),
                                iconBg = if (s.overdue) AtColors.DangerSoft else AtColors.Card,
                                iconTint = if (s.overdue) AtColors.Danger else AtColors.Ink,
                                trailing = {
                                    AtPillButton(
                                        "Já fiz",
                                        { scope.launch { app.graph.maintenanceRepository.markDone(vehicleId, s.itemKey, System.currentTimeMillis()) } },
                                        style = AtButtonStyle.Accent,
                                        compact = true
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Histórico", "Do mais recente ao mais antigo")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (records.isEmpty()) {
                        AtListRow(
                            title = "Nenhuma manutenção registrada",
                            subtitle = "Toque para registrar a primeira",
                            icon = Icons.Rounded.Build,
                            onClick = onAdd
                        )
                    }
                    records.forEach { r ->
                        AtListRow(
                            title = MaintenanceCatalog.label(r.itemKey),
                            subtitle = "${df.format(Date(r.eventDateMillis))} · ${formatKm(r.eventKm)} km" +
                                (r.laborAmount?.let { l ->
                                    "\nPeças ${formatMoney(MaintenanceCost.parts(r.amount, l) ?: 0.0)} + mão de obra ${formatMoney(l)}"
                                } ?: "") +
                                (r.description?.let { "\n$it" } ?: ""),
                            subtitleLines = 3,
                            icon = maintenanceIcon(r.itemKey),
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    r.receiptPath?.let { path ->
                                        IconButton(onClick = { ReceiptFileStore.open(context, path) }) {
                                            Icon(Icons.Rounded.AttachFile, "Abrir comprovante", tint = AtColors.Primary)
                                        }
                                    }
                                    Text(
                                        r.amount?.let { formatMoney(it) } ?: "—",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        AtPillButton("Nova manutenção", onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMaintenanceScreen(
    vehicleId: Long,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    pendingId: Long = -1L
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val scope = rememberCoroutineScope()
    var itemKey by remember { mutableStateOf<String?>(null) }
    // itens próprios já criados pelo usuário neste veículo (RF-04)
    val recordsFlow = remember(vehicleId) { app.graph.maintenanceRepository.observeRecords(vehicleId) }
    val existingRecords by recordsFlow.collectAsState(initial = emptyList())
    val customKeys = remember(existingRecords) {
        existingRecords.map { it.itemKey }.filter { MaintenanceCatalog.isCustom(it) }.distinct()
    }
    var eventKm by remember { mutableStateOf("") }
    var vehicleKm by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // RF-04: valor das peças e da mão de obra separados (opcional)
    var splitLabor by remember { mutableStateOf(false) }
    var partsValue by remember { mutableStateOf("") }
    var laborValue by remember { mutableStateOf("") }
    // comprovante escolhido (qualquer tipo de arquivo), guardado de vez ao salvar
    var pending by remember { mutableStateOf<PendingReceipt?>(null) }
    // RF-04: só o KM do dia do serviço é obrigatório; marcado como KM atual, ele atualiza o odômetro
    var isCurrent by remember { mutableStateOf(true) }
    val vehicleFlow = remember(vehicleId) { app.graph.vehicleRepository.observeVehicle(vehicleId) }
    val vehicle by vehicleFlow.collectAsState(initial = null)
    LaunchedEffect(Unit) { ReceiptFileStore.cleanupPending(context) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var intervalKm by remember { mutableStateOf("") }
    var intervalMonths by remember { mutableStateOf("") }
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

    // RF-04 / RF-11: revisão com vários itens; intervalo em comum (junção) ou próprio (separação)
    val isReview = itemKey in MaintenanceCatalog.REVIEW_KEYS
    var reviewItems by remember { mutableStateOf(listOf<String>()) }
    var ownInterval by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) }
    var groupKm by remember { mutableStateOf("") }
    var groupMonths by remember { mutableStateOf("") }
    var addQuery by remember { mutableStateOf("") }

    // RF-06: completar um item marcado como "Já fiz": já vem com o item, a data em que foi feito
    // e o intervalo do último ciclo; faltam km, valor, descrição e comprovante.
    var completing by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingId) {
        if (pendingId <= 0) return@LaunchedEffect
        val p = app.graph.maintenanceRepository.getPending(pendingId) ?: return@LaunchedEffect
        itemKey = p.itemKey
        eventDate = p.doneDateMillis
        sameDay = false
        registeredDate = System.currentTimeMillis()
        app.graph.maintenanceRepository.lastCycleFor(vehicleId, p.itemKey)?.let { c ->
            intervalKm = c.intervalKm?.toString().orEmpty()
            intervalMonths = c.intervalMonths?.toString().orEmpty()
        }
        completing = "Você marcou ${MaintenanceCatalog.label(p.itemKey)} como feita em " +
            SimpleDateFormat("dd/MM/yyyy", PT_BR).format(Date(p.doneDateMillis)) +
            ". Complete a quilometragem e, se quiser, o valor, a descrição e o comprovante."
    }

    fun save() {
        val key = itemKey ?: run {
            error = "Escolha o item de manutenção (parte do carro e item)."
            return
        }
        if (!dayIsBeforeOrEqual(eventDate, registeredDate)) {
            error = "A data do serviço não pode ser depois da data do registro."
            return
        }
        val ek = eventKm.toLongOrNull() ?: run {
            error = "Informe o KM no dia do serviço."
            return
        }
        // RN-02 / RN-05: o KM atual é opcional; sem ele, o odômetro do veículo não muda
        val typedCurrent: Long? = if (isCurrent) ek else vehicleKm.toLongOrNull()
        val vk = typedCurrent ?: maxOf(vehicle?.currentOdometerKm ?: ek, ek)
        val am = if (splitLabor) MaintenanceCost.total(parseDecimal(partsValue), parseDecimal(laborValue)) else parseDecimal(amount)
        val labor = if (splitLabor) parseDecimal(laborValue) else null
        scope.launch {
            saving = true
            error = null
            val base = ReceiptFileStore.baseName("Manutencao", vehicle?.shortName, vehicle?.plate, MaintenanceCatalog.label(key), eventDate)
            val receiptPath = pending?.let { ReceiptFileStore.finalize(context, it, base) }
            val record = MaintenanceRecordEntity(
                vehicleId = vehicleId,
                itemKey = key,
                eventDateMillis = eventDate,
                registeredAtMillis = registeredDate,
                eventKm = ek,
                vehicleKmAtRecord = vk,
                amount = am,
                laborAmount = labor,
                description = description.ifBlank { null },
                receiptPath = receiptPath
            )
            app.graph.maintenanceRepository.addRecord(record, updateVehicleOdometerTo = typedCurrent)
                .onSuccess { maintId ->
                    val ik = intervalKm.toIntOrNull()
                    val im = intervalMonths.toIntOrNull()
                    if (ik != null || im != null) {
                        app.graph.maintenanceRepository.addCustomCycle(
                            vehicleId = vehicleId,
                            maintenanceId = maintId,
                            itemKey = key,
                            intervalKm = ik,
                            intervalMonths = im
                        )
                    }
                    // itens feitos na revisão: um registro por item, com o intervalo em comum ou o próprio
                    if (key in MaintenanceCatalog.REVIEW_KEYS) {
                        val note = "Feito na ${MaintenanceCatalog.label(key).lowercase(PT_BR)} de " +
                            SimpleDateFormat("dd/MM/yyyy", PT_BR).format(Date(eventDate))
                        for (item in reviewItems) {
                            val sub = record.copy(id = 0, itemKey = item, amount = null, laborAmount = null, description = note, receiptPath = null)
                            app.graph.maintenanceRepository.addRecord(sub, updateVehicleOdometerTo = typedCurrent)
                                .onSuccess { subId ->
                                    val own = ownInterval[item]
                                    val km = if (own != null) own.first.toIntOrNull() else groupKm.toIntOrNull()
                                    val months = if (own != null) own.second.toIntOrNull() else groupMonths.toIntOrNull()
                                    if (km != null || months != null) {
                                        app.graph.maintenanceRepository.addCustomCycle(vehicleId, subId, item, km, months)
                                    }
                                }
                        }
                    }
                    onDone()
                }
                .onFailure {
                    // o registro não foi salvo: o arquivo volta para a área de espera
                    receiptPath?.let { path -> pending?.let { pr -> ReceiptFileStore.revert(path, pr) } }
                    error = it.message
                }
            saving = false
        }
    }

    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)

    AtScreen {
        AtHeader(if (completing != null) "Completar manutenção" else "Nova manutenção", "Registro de serviço", onBack = onCancel)
        completing?.let { AtMessage(it, isError = false) }

        Reveal(0) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Serviço", "Escolha a parte do carro e o item")
                Spacer(Modifier.height(14.dp))
                MaintenanceItemPicker(
                    selectedKey = itemKey,
                    onSelect = { itemKey = it },
                    fuel = vehicle?.fuelType,
                    customKeys = customKeys
                )
            }
        }

        if (isReview) {
            Reveal(0) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Itens da revisão", "Marque o que foi feito ou verificado")
                    Spacer(Modifier.height(10.dp))
                    val suggestions = MaintenanceCatalog.reviewChecklist(vehicle?.fuelType).map { it.key }
                    val extra = reviewItems.filter { it !in suggestions }
                    Column {
                        (suggestions + extra).forEach { k ->
                            AtCheckRow(MaintenanceCatalog.label(k), k in reviewItems) { on ->
                                reviewItems = if (on) reviewItems + k else reviewItems - k
                                if (!on) ownInterval = ownInterval - k
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AtField(addQuery, { addQuery = it }, "Adicionar outro item", placeholder = "Ex.: correia, pastilha, bateria", leadingIcon = Icons.Rounded.Add)
                    if (addQuery.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MaintenanceCatalog.search(addQuery, vehicle?.fuelType)
                                .filter { it.key !in MaintenanceCatalog.REVIEW_KEYS }
                                .take(8)
                                .forEach { item ->
                                    AtChip(item.label, selected = item.key in reviewItems, onClick = {
                                        if (item.key !in reviewItems) reviewItems = reviewItems + item.key
                                        addQuery = ""
                                    })
                                }
                        }
                    }
                    if (reviewItems.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        AtLabel("Intervalo em comum dos itens marcados")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AtField(groupKm, { groupKm = it.filter { c -> c.isDigit() } }, "Em (km)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                            AtField(groupMonths, { groupMonths = it.filter { c -> c.isDigit() } }, "Em (meses)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Os itens marcados ficam juntos nesse intervalo. Para separar um item com prazo diferente, marque \u201cintervalo próprio\u201d.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtColors.Muted
                        )
                        Spacer(Modifier.height(6.dp))
                        reviewItems.forEach { k ->
                            val own = ownInterval[k]
                            AtCheckRow("${MaintenanceCatalog.label(k)}: intervalo próprio", own != null) { on ->
                                ownInterval = if (on) ownInterval + (k to ("" to "")) else ownInterval - k
                            }
                            if (own != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)) {
                                    AtField(own.first, { v ->
                                        val cur = ownInterval[k] ?: ("" to "")
                                        ownInterval = ownInterval + (k to (v.filter { c -> c.isDigit() } to cur.second))
                                    }, "Em (km)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                                    AtField(own.second, { v ->
                                        val cur = ownInterval[k] ?: ("" to "")
                                        ownInterval = ownInterval + (k to (cur.first to v.filter { c -> c.isDigit() }))
                                    }, "Em (meses)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Datas", "Quando o serviço foi feito e quando está sendo registrado")
                Spacer(Modifier.height(10.dp))
                AtEventDates(
                    eventLabel = "Serviço",
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

        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Quilometragem", "Odômetro registrado: ${vehicle?.let { formatKm(it.currentOdometerKm) + " km" } ?: "\u2014"}")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtField(eventKm, { eventKm = it.filter { c -> c.isDigit() } }, "KM no dia do serviço", leadingIcon = Icons.Rounded.Speed, keyboardOptions = numeric)
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

        Reveal(2) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Detalhes")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtCheckRow("Especificar mão de obra", splitLabor) { on ->
                        splitLabor = on
                        if (on && partsValue.isBlank()) partsValue = amount
                        if (!on && amount.isBlank()) {
                            MaintenanceCost.total(parseDecimal(partsValue), parseDecimal(laborValue))?.let { amount = String.format(PT_BR, "%.2f", it) }
                        }
                    }
                    if (splitLabor) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AtField(
                                partsValue, { partsValue = it }, "Peças / produtos",
                                placeholder = "0,00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AtField(
                                laborValue, { laborValue = it }, "Mão de obra",
                                placeholder = "0,00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            "Total: " + formatMoney(MaintenanceCost.total(parseDecimal(partsValue), parseDecimal(laborValue)) ?: 0.0),
                            style = MaterialTheme.typography.titleSmall
                        )
                    } else {
                        AtField(
                            amount, { amount = it }, "Valor total (opcional)",
                            placeholder = "0,00",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text(
                            "Se a peça e o serviço vieram num valor só, deixe assim. Para separar a mão de obra, marque a caixa acima.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtColors.Muted
                        )
                    }
                    AtField(description, { description = it }, "Descrição", singleLine = false)
                    ReceiptAttachment(
                        pending = pending,
                        onChange = { pending = it },
                        finalBaseName = ReceiptFileStore.baseName("Manutencao", vehicle?.shortName, vehicle?.plate, itemKey?.let { MaintenanceCatalog.label(it) } ?: "Item", eventDate)
                    )
                }
            }
        }

        Reveal(3) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle(if (isReview) "Alerta da revisão" else "Alerta personalizado", "Opcional · avisa quando chegar a hora") {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = AtColors.Ink)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AtField(intervalKm, { intervalKm = it.filter { c -> c.isDigit() } }, "Em (km)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                    AtField(intervalMonths, { intervalMonths = it.filter { c -> c.isDigit() } }, "Em (meses)", keyboardOptions = numeric, modifier = Modifier.weight(1f))
                }
            }
        }

        error?.let { AtMessage(it, isError = true) }

        AtPillButton("Salvar manutenção", { save() }, Modifier.fillMaxWidth(), loading = saving)
        AtPillButton("Cancelar", onCancel, Modifier.fillMaxWidth(), style = AtButtonStyle.Light)
        Spacer(Modifier.height(8.dp))
    }
}
