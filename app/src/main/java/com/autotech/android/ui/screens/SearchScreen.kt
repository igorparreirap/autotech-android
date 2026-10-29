package com.autotech.android.ui.screens

import com.autotech.android.ui.components.fuelLabel

import com.autotech.android.ui.components.shortName
import com.autotech.android.ui.components.displayName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.MaintenanceCatalog
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.maintenanceIcon
import com.autotech.android.ui.navigation.NavRoutes
import com.autotech.android.ui.theme.AtColors
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date

/** Função do app que pode ser encontrada pela busca. */
private data class AppFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val keywords: String,
    /** true = abre a tela de um veículo específico (gera um resultado por veículo) */
    val perVehicle: Boolean,
    val route: (Long?) -> String
)

private val FEATURES = listOf(
    AppFeature("Odômetro", "Ver e atualizar a quilometragem", Icons.Rounded.Speed,
        "odometro km quilometragem quilometro atualizar painel", true) { NavRoutes.vehicleDetail(it!!) },
    AppFeature("Meu veículo", "Ficha, placa e foto do veículo", Icons.Rounded.DirectionsCar,
        "meu veiculo carro detalhes ficha placa ano cor versao nome apelido renomear", true) { NavRoutes.vehicleDetail(it!!) },
    AppFeature("Foto do veículo", "Adicionar, trocar ou remover a foto", Icons.Rounded.AddAPhoto,
        "foto imagem camera galeria", true) { NavRoutes.vehicleDetail(it!!) },
    AppFeature("Relatório em PDF", "Veículo, manutenções, abastecimentos e comprovantes", Icons.Rounded.PictureAsPdf,
        "pdf relatorio exportar historico documento venda gerar compartilhar", true) { NavRoutes.vehicleDetail(it!!, openPdf = true) },
    AppFeature("Manutenções", "Histórico de serviços do veículo", Icons.Rounded.Build,
        "manutencao manutencoes revisao servico historico oleo freio pneu bateria correia", true) { NavRoutes.maintenanceList(it!!) },
    AppFeature("Nova manutenção", "Registrar um serviço feito", Icons.Rounded.Add,
        "nova manutencao registrar servico lancar adicionar revisao ciclo alerta", true) { NavRoutes.maintenanceAdd(it!!) },
    AppFeature("Abastecimentos", "Histórico de abastecimentos", Icons.Rounded.LocalGasStation,
        "abastecimento abastecimentos combustivel gasolina alcool etanol litros posto", true) { NavRoutes.fuelList(it!!) },
    AppFeature("Novo abastecimento", "Registrar um abastecimento", Icons.Rounded.Add,
        "novo abastecimento registrar abastecer lancar combustivel", true) { NavRoutes.fuelAdd(it!!) },
    AppFeature("Calculadora álcool ou gasolina", "Qual combustível compensa hoje", Icons.Rounded.Calculate,
        "calculadora alcool gasolina etanol compensa 70 preco comparar", true) { NavRoutes.fuelList(it!!) },
    AppFeature("Veículos", "Todos os seus veículos", Icons.Rounded.DirectionsCar,
        "veiculos carros lista garagem", false) { NavRoutes.VEHICLES },
    AppFeature("Novo veículo", "Cadastrar um veículo pela placa", Icons.Rounded.Add,
        "novo veiculo cadastrar adicionar carro placa", false) { NavRoutes.VEHICLE_ADD },
    AppFeature("Dashboard", "Gastos por período, categoria e km", Icons.Rounded.Insights,
        "dashboard gastos financeiro painel grafico despesas custo consumo relatorio", false) { NavRoutes.DASHBOARD },
    AppFeature("Notificações", "Manutenções vencidas e próximas", Icons.Rounded.NotificationsNone,
        "notificacoes notificacao alertas alerta lembretes avisos sino vencida", false) { NavRoutes.NOTIFICATIONS },
    AppFeature("Localização", "Oficinas, borracharias e postos perto", Icons.Rounded.Map,
        "localizacao mapa oficina borracharia posto mecanico servicos perto rota", false) { NavRoutes.MAP }
)

private val SUGGESTED = listOf("Odômetro", "Meu veículo", "Dashboard", "Localização", "Notificações")

/** minúsculas e sem acento: "Odômetro" -> "odometro" */
private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(PT_BR), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private fun matches(query: List<String>, haystack: String): Boolean {
    val h = norm(haystack)
    return query.all { h.contains(it) }
}

/** Busca no aplicativo: funções (odômetro, abastecimento, dashboard...) e dados (veículos e manutenções). */
@Composable
fun SearchScreen(
    userId: Long,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val data by produceState<Pair<List<VehicleEntity>, List<MaintenanceRecordEntity>>?>(null, userId) {
        val vs = app.graph.vehicleRepository.listVehicles(userId)
        value = vs to vs.flatMap { app.graph.maintenanceRepository.listForPdf(it.id) }
    }
    val vehicles = data?.first.orEmpty()
    val records = data?.second.orEmpty()
    val names = vehicles.associate { it.id to "${it.shortName} · ${it.plate}" }
    val df = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }

    val terms = norm(query).split(Regex("\\s+")).filter { it.isNotBlank() }

    fun go(route: String) {
        keyboard?.hide()
        onNavigate(route)
    }

    AtScreen {
        AtHeader("Buscar", "Funções, veículos e manutenções", onBack = onBack)

        AtField(
            value = query,
            onValueChange = { query = it },
            label = "O que você procura?",
            placeholder = "Ex.: odômetro, troca de óleo, placa...",
            leadingIcon = Icons.Rounded.Search,
            trailing = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Limpar") } }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.focusRequester(focus)
        )

        // ── Funções do app ───────────────────────────────────────────────────
        val features = if (terms.isEmpty()) FEATURES.filter { it.title in SUGGESTED }
        else FEATURES.filter { matches(terms, "${it.title} ${it.description} ${it.keywords}") }

        // funções de um veículo: um resultado por veículo (ou cadastro, se não houver veículo)
        val featureRows = features.flatMap { f ->
            when {
                !f.perVehicle -> listOf(Triple(f, f.description, f.route(null)))
                vehicles.isEmpty() -> listOf(Triple(f, "Cadastre um veículo primeiro", NavRoutes.VEHICLE_ADD))
                else -> vehicles.map { v -> Triple(f, "${v.shortName} · ${v.plate}", f.route(v.id)) }
            }
        }

        // ── Dados: veículos e manutenções ────────────────────────────────────
        val vehicleHits = if (terms.isEmpty()) emptyList()
        else vehicles.filter { v ->
            matches(terms, "${v.nickname.orEmpty()} ${v.brand} ${v.model} ${v.version.orEmpty()} ${v.plate} ${v.year ?: ""} ${v.color.orEmpty()} ${v.fuel.orEmpty()} ${v.fuelLabel.orEmpty()}")
        }
        val recordHits = if (terms.isEmpty()) emptyList()
        else records.filter { r ->
            matches(terms, "${MaintenanceCatalog.label(r.itemKey)} ${r.description.orEmpty()} ${df.format(Date(r.eventDateMillis))}")
        }.take(10)

        if (featureRows.isNotEmpty()) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle(if (terms.isEmpty()) "Sugestões" else "Funções do app")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    featureRows.forEach { (f, sub, route) ->
                        AtListRow(
                            title = f.title,
                            subtitle = sub,
                            icon = f.icon,
                            onClick = { go(route) },
                            trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted) }
                        )
                    }
                }
            }
        }

        if (vehicleHits.isNotEmpty()) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Veículos")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehicleHits.forEach { v ->
                        AtListRow(
                            title = v.displayName,
                            subtitle = "${v.plate} · ${formatKm(v.currentOdometerKm)} km",
                            icon = Icons.Rounded.DirectionsCar,
                            onClick = { go(NavRoutes.vehicleDetail(v.id)) },
                            trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtColors.Muted) }
                        )
                    }
                }
            }
        }

        if (recordHits.isNotEmpty()) {
            AtCard(Modifier.fillMaxWidth()) {
                AtCardTitle("Manutenções registradas")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recordHits.forEach { r ->
                        AtListRow(
                            title = MaintenanceCatalog.label(r.itemKey),
                            subtitle = "${names[r.vehicleId] ?: ""} · ${df.format(Date(r.eventDateMillis))} · ${formatKm(r.eventKm)} km",
                            icon = maintenanceIcon(r.itemKey),
                            onClick = { go(NavRoutes.maintenanceList(r.vehicleId)) },
                            trailing = { Text(r.amount?.let { formatMoney(it) } ?: "—", style = MaterialTheme.typography.titleSmall) }
                        )
                    }
                }
            }
        }

        if (terms.isNotEmpty() && featureRows.isEmpty() && vehicleHits.isEmpty() && recordHits.isEmpty()) {
            AtCard(Modifier.fillMaxWidth()) {
                AtListRow(
                    title = "Nada encontrado para \"$query\"",
                    subtitle = "Tente outra palavra, como \"óleo\", \"placa\" ou \"gastos\"",
                    icon = Icons.Rounded.SearchOff
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
