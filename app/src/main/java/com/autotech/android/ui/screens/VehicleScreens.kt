package com.autotech.android.ui.screens

import com.autotech.android.ui.components.displayName
import com.autotech.android.ui.components.shortName
import com.autotech.android.ui.components.originalName
import com.autotech.android.ui.components.fuelLabel
import com.autotech.android.ui.components.fuelType
import com.autotech.android.ui.components.FuelTypePicker
import com.autotech.android.domain.FuelType
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.rounded.DeleteOutline
import com.autotech.android.util.VehiclePhotoStore
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.PlateRules
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtCarStage
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtLabel
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtPlate
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.AtStat
import com.autotech.android.ui.components.AtTileGrid
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.TileSpec
import com.autotech.android.ui.components.VehiclePhotoStage
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.VehicleReportPdf
import com.autotech.android.ui.components.AtCheckRow
import com.autotech.android.ui.components.AtListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable
fun VehicleListScreen(
    userId: Long,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    val flow = remember(userId) { app.graph.vehicleRepository.observeVehicles(userId) }
    val vehicles by flow.collectAsState(initial = null)

    AtScreen {
        AtHeader(
            title = "Veículos",
            subtitle = vehicles?.let { "${it.size} cadastrado(s)" },
            onBack = onBack
        ) {
            AtCircleButton(Icons.Rounded.Add, "Adicionar veículo", onAdd, dark = true)
        }

        val list = vehicles
        if (list != null && list.isEmpty()) {
            Reveal(0) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCarStage(iconSize = 130.dp, icon = Icons.Rounded.DirectionsCar)
                    Text(
                        "Nenhum veículo cadastrado",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Comece pela placa: os dados são preenchidos automaticamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    AtPillButton("Adicionar veículo", onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                }
            }
        }

        list.orEmpty().forEachIndexed { i, v ->
            Reveal(i + 1) {
                AtCard(Modifier.fillMaxWidth(), onClick = { onOpen(v.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(AtColors.Primary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(v.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                (if (v.nickname != null) v.originalName else listOfNotNull(v.version, v.year?.toString()).joinToString(" · ").ifEmpty { "Sem versão" }),
                                style = MaterialTheme.typography.bodySmall,
                                color = AtColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtPlate(v.plate, compact = true)
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            AtLabel("Odômetro")
                            Text("${formatKm(v.currentOdometerKm)} km", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddVehicleScreen(
    userId: Long,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    val scope = rememberCoroutineScope()

    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var modelYear by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf<FuelType?>(null) }
    var odometer by remember { mutableStateOf("") }

    var searching by remember { mutableStateOf(false) }
    var lookupMessage by remember { mutableStateOf<String?>(null) }
    var lookupOk by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun searchPlate() {
        scope.launch {
            searching = true
            lookupMessage = null
            app.graph.plateLookupRepository.lookup(plate)
                .onSuccess { d ->
                    brand = d.brand
                    model = d.model
                    version = d.version.orEmpty()
                    year = d.year?.toString().orEmpty()
                    modelYear = d.modelYear?.toString().orEmpty()
                    color = d.color.orEmpty()
                    fuelType = FuelType.from(d.fuel)
                    lookupOk = true
                    lookupMessage = if (fuelType == null)
                        "Dados encontrados. A consulta não informou o combustível: escolha abaixo."
                    else "Dados encontrados. Confira e ajuste se necessário."
                }
                .onFailure {
                    lookupOk = false
                    lookupMessage = "${it.message} Preencha os dados manualmente."
                }
            searching = false
        }
    }

    fun save() {
        if (!PlateRules.isValid(plate)) {
            saveError = "Informe uma placa válida (ABC1D23 ou ABC1234)."
            return
        }
        if (brand.isBlank() || model.isBlank()) {
            saveError = "Informe marca e modelo (busque pela placa ou preencha manualmente)."
            return
        }
        val fuelChoice = fuelType ?: run {
            saveError = "Escolha o combustível do veículo."
            return
        }
        val km = odometer.toLongOrNull() ?: run {
            saveError = "KM inválido."
            return
        }
        scope.launch {
            saving = true
            saveError = null
            val entity = VehicleEntity(
                userId = userId,
                plate = PlateRules.normalize(plate),
                brand = brand.trim(),
                model = model.trim(),
                version = version.trim().ifEmpty { null },
                year = year.toIntOrNull(),
                modelYear = modelYear.toIntOrNull(),
                color = color.trim().ifEmpty { null },
                fuel = fuelChoice.name,
                currentOdometerKm = km
            )
            app.graph.vehicleRepository.insertVehicle(entity)
                .onSuccess { onDone() }
                .onFailure { saveError = it.message }
            saving = false
        }
    }

    AtScreen {
        AtHeader("Novo veículo", "Cadastro pela placa", onBack = onCancel)

        Reveal(0) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Buscar pela placa", "Os dados do veículo são preenchidos automaticamente")
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AtPlate(plate)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtField(
                        value = plate,
                        onValueChange = { plate = PlateRules.normalize(it).take(7) },
                        label = "Placa",
                        placeholder = "ABC1D23",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { if (!searching && PlateRules.isValid(plate)) searchPlate() }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    AtPillButton(
                        text = "Buscar",
                        onClick = { searchPlate() },
                        enabled = PlateRules.isValid(plate),
                        loading = searching,
                        icon = Icons.Rounded.Search
                    )
                }
                lookupMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    AtMessage(it, isError = !lookupOk)
                }
            }
        }

        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Dados do veículo", "Confira antes de salvar")
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtField(brand, { brand = it }, "Marca")
                    AtField(model, { model = it }, "Modelo")
                    AtField(version, { version = it }, "Versão")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AtField(
                            year, { year = it.filter(Char::isDigit).take(4) }, "Ano fabr.",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        AtField(
                            modelYear, { modelYear = it.filter(Char::isDigit).take(4) }, "Ano modelo",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AtField(color, { color = it }, "Cor")
                    Spacer(Modifier.height(2.dp))
                    AtLabel("Combustível")
                    FuelTypePicker(fuelType, { fuelType = it })
                }
            }
        }

        Reveal(2) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Quilometragem", "Valor atual do painel do carro")
                Spacer(Modifier.height(14.dp))
                AtField(
                    value = odometer,
                    onValueChange = { odometer = it.filter(Char::isDigit) },
                    label = "KM atual do veículo",
                    leadingIcon = Icons.Rounded.Speed,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        saveError?.let { AtMessage(it, isError = true) }

        AtPillButton(
            "Salvar veículo", { save() }, Modifier.fillMaxWidth(),
            enabled = !searching, loading = saving
        )
        AtPillButton("Cancelar", onCancel, Modifier.fillMaxWidth(), style = AtButtonStyle.Light)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun VehicleDetailScreen(
    vehicleId: Long,
    onBack: () -> Unit,
    onMaintenance: () -> Unit,
    onFuel: () -> Unit,
    onDeleted: () -> Unit = {},
    onOpenPdf: (String) -> Unit = {},
    openPdfOnStart: Boolean = false
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val flow = remember(vehicleId) { app.graph.vehicleRepository.observeVehicle(vehicleId) }
    val vehicle by flow.collectAsState(initial = null)
    var showKmDialog by remember { mutableStateOf(false) }
    var kmInput by remember { mutableStateOf("") }
    var kmError by remember { mutableStateOf<String?>(null) }
    var pdfMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showFuel by remember { mutableStateOf(false) }
    var fuelChoice by remember { mutableStateOf<FuelType?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var deleteAll by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // RF-10: opções do relatório e relatórios já gerados
    var showPdf by remember { mutableStateOf(openPdfOnStart) }
    var report by remember { mutableStateOf<java.io.File?>(null) }

    // RF-10: um relatório por veículo; atualizado quando a janela do PDF fecha
    LaunchedEffect(vehicleId, showPdf) {
        report = withContext(Dispatchers.IO) { VehicleReportPdf.currentReport(context, vehicleId) }
    }


    AtScreen {
        AtHeader("Detalhes do veículo", onBack = onBack)

        val v = vehicle
        if (v == null) {
            AtCard(Modifier.fillMaxWidth().height(360.dp)) {}
        } else {

        // ── Painel principal ("Car Details") ────────────────────────────────
        Reveal(0) {
            AtCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ficha", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .background(AtColors.Field, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(v.fuelLabel ?: "Combustível não informado", style = MaterialTheme.typography.labelSmall, color = AtColors.Muted)
                    }
                }
                Spacer(Modifier.height(18.dp))
                // RF-03: nome do veículo, com opção de renomear
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        v.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = { nameInput = v.displayName; showRename = true }) {
                        Icon(Icons.Rounded.Edit, "Renomear veículo", tint = AtColors.Primary)
                    }
                }
                if (v.nickname != null) {
                    Text(
                        "Nome original: ${v.originalName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                v.version?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtColors.Faint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                VehiclePhotoStage(v.id, Modifier.padding(vertical = 10.dp), height = 190.dp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AtPlate(v.plate) }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(AtColors.Field, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    val anos = listOfNotNull(v.year, v.modelYear).distinct().joinToString("/")
                    AtStat("Ano", anos.ifEmpty { "—" }, Modifier.weight(1f))
                    AtStat("Cor", v.color ?: "—", Modifier.weight(1f))
                    // RF-03: toque para informar ou corrigir o combustível
                    AtStat(
                        "Combustível ✎",
                        v.fuelLabel ?: "Informar",
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { fuelChoice = v.fuelType; showFuel = true }
                    )
                }
            }
        }

        // ── Odômetro ────────────────────────────────────────────────────────
        Reveal(1) {
            AtCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Odômetro", style = MaterialTheme.typography.titleMedium)
                        AtLabel("Quilometragem atual")
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(formatKm(v.currentOdometerKm), style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.width(6.dp))
                            Text("km", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                    AtPillButton(
                        "Atualizar",
                        onClick = {
                            kmInput = v.currentOdometerKm.toString()
                            kmError = null
                            showKmDialog = true
                        },
                        leadingIcon = Icons.Rounded.Edit,
                        compact = true
                    )
                }
            }
        }

        // ── Ações ───────────────────────────────────────────────────────────
        Reveal(2) {
            AtTileGrid(
                listOf(
                    TileSpec("Manutenções", Icons.Rounded.Build, onMaintenance),
                    TileSpec("Combustível", Icons.Rounded.LocalGasStation, onFuel),
                    TileSpec("Gerar PDF", Icons.Rounded.PictureAsPdf) { showPdf = true }
                )
            )
        }

        pdfMessage?.let { (msg, isErr) -> AtMessage(msg, isError = isErr) }

        report?.let { f ->
            Reveal(3) {
                AtCard(Modifier.fillMaxWidth()) {
                    AtCardTitle("Relatório do veículo", "Se algo mudou desde então, um novo é gerado")
                    Spacer(Modifier.height(12.dp))
                    val fmt = remember { java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", java.util.Locale("pt", "BR")) }
                    AtListRow(
                        title = "Gerado em ${fmt.format(java.util.Date(f.lastModified()))}",
                        subtitle = "PDF · ${(f.length() / 1024).coerceAtLeast(1)} KB",
                        icon = Icons.Rounded.PictureAsPdf,
                        iconTint = AtColors.Primary,
                        onClick = { showPdf = true },
                        trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted) }
                    )
                }
            }
        }

        // RF-03: excluir veículo
        Reveal(3) {
            AtPillButton(
                "Excluir veículo",
                { deleteAll = false; showDelete = true },
                Modifier.fillMaxWidth(),
                style = AtButtonStyle.Light,
                leadingIcon = Icons.Rounded.DeleteOutline
            )
        }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showPdf) {
        vehicle?.let { v ->
            VehicleReportDialog(
                vehicle = v,
                onDismiss = { showPdf = false },
                onGenerated = { file -> showPdf = false; onOpenPdf(file.absolutePath) }
            )
        }
    }

    if (showFuel) {
        AlertDialog(
            onDismissRequest = { showFuel = false },
            containerColor = AtColors.Card,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Combustível", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Qual combustível este veículo usa?", style = MaterialTheme.typography.bodyMedium)
                    FuelTypePicker(fuelChoice, { fuelChoice = it })
                }
            },
            confirmButton = {
                AtPillButton("Salvar", {
                    val choice = fuelChoice ?: return@AtPillButton
                    scope.launch {
                        app.graph.vehicleRepository.setFuelType(vehicleId, choice)
                        showFuel = false
                    }
                }, compact = true, enabled = fuelChoice != null)
            },
            dismissButton = {
                AtPillButton("Cancelar", { showFuel = false }, style = AtButtonStyle.Soft, compact = true)
            }
        )
    }

    if (showRename) {
        val v = vehicle
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = AtColors.Card,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Nome do veículo", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtField(
                        nameInput, { nameInput = it.take(40) },
                        "Como você chama este veículo?",
                        placeholder = v?.originalName,
                        leadingIcon = Icons.Rounded.Edit
                    )
                    Text(
                        "Nome da consulta da placa: ${v?.originalName.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                    if (v?.nickname != null) {
                        AtPillButton(
                            "Restaurar nome original",
                            {
                                scope.launch {
                                    app.graph.vehicleRepository.renameVehicle(vehicleId, null)
                                    showRename = false
                                }
                            },
                            Modifier.fillMaxWidth(),
                            style = AtButtonStyle.Soft,
                            leadingIcon = Icons.Rounded.Restore
                        )
                    }
                }
            },
            confirmButton = {
                AtPillButton("Salvar", {
                    scope.launch {
                        app.graph.vehicleRepository.renameVehicle(vehicleId, nameInput)
                        showRename = false
                    }
                }, compact = true)
            },
            dismissButton = {
                AtPillButton("Cancelar", { showRename = false }, style = AtButtonStyle.Soft, compact = true)
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDelete = false },
            containerColor = AtColors.Card,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Excluir ${vehicle?.shortName ?: "veículo"}?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("O veículo sai da sua lista de veículos.", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { deleteAll = !deleteAll },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteAll,
                            onCheckedChange = { deleteAll = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFC62828),
                                uncheckedColor = AtColors.Muted,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            "Apagar também todas as manutenções, abastecimentos e lembretes deste veículo",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    AtMessage(
                        if (deleteAll) "Tudo será apagado de vez, inclusive a foto e os comprovantes. Essa ação não pode ser desfeita."
                        else "O histórico de manutenções e gastos continua guardado e aparece no Dashboard. Se você cadastrar a mesma placa de novo, ele volta.",
                        isError = deleteAll
                    )
                }
            },
            confirmButton = {
                AtPillButton(
                    if (deleteAll) "Apagar tudo" else "Excluir",
                    {
                        scope.launch {
                            deleting = true
                            app.graph.vehicleRepository.deleteVehicle(vehicleId, deleteAll)
                                .onSuccess {
                                    if (deleteAll) {
                                        VehiclePhotoStore.delete(context, vehicleId)
                                        vehicle?.let { VehicleReportPdf.deleteReports(context, vehicleId, it.plate) }
                                    }
                                    showDelete = false
                                    onDeleted()
                                }
                                .onFailure {
                                    deleting = false
                                    showDelete = false
                                    pdfMessage = (it.message ?: "Não foi possível excluir o veículo.") to true
                                }
                        }
                    },
                    style = AtButtonStyle.Danger,
                    compact = true,
                    loading = deleting
                )
            },
            dismissButton = {
                AtPillButton("Cancelar", { showDelete = false }, style = AtButtonStyle.Soft, compact = true, enabled = !deleting)
            }
        )
    }

    if (showKmDialog) {
        AlertDialog(
            onDismissRequest = { showKmDialog = false },
            containerColor = AtColors.Card,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Atualizar odômetro", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtField(
                        value = kmInput,
                        onValueChange = { kmInput = it.filter { c -> c.isDigit() } },
                        label = "KM atual",
                        leadingIcon = Icons.Rounded.Speed,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    kmError?.let { AtMessage(it, isError = true) }
                }
            },
            confirmButton = {
                AtPillButton(
                    "Salvar",
                    onClick = {
                        val n = kmInput.toLongOrNull() ?: return@AtPillButton
                        scope.launch {
                            app.graph.vehicleRepository.updateOdometer(vehicleId, n)
                                .onSuccess { showKmDialog = false }
                                .onFailure { kmError = it.message }
                        }
                    },
                    compact = true
                )
            },
            dismissButton = {
                AtPillButton("Cancelar", { showKmDialog = false }, style = AtButtonStyle.Soft, compact = true)
            }
        )
    }
}

