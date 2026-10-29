package com.autotech.android.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.data.mapper.toExpense
import com.autotech.android.domain.Expense
import com.autotech.android.domain.SpendGrouping
import com.autotech.android.domain.SpendBucket
import com.autotech.android.domain.SpendPeriod
import com.autotech.android.domain.SpendingAnalytics
import com.autotech.android.ui.components.shortName
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtChip
import com.autotech.android.ui.components.AtDateField
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtLabel
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.Reveal
import com.autotech.android.ui.components.formatKm
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.maintenanceIcon
import com.autotech.android.ui.theme.AtColors
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// ─── Modelo da tela ──────────────────────────────────────────────────────────
// Os cálculos ficam no domínio (domain/SpendingAnalytics); aqui só ficam rótulos e cores.

private enum class Period(val label: String, val kind: SpendPeriod) {
    MONTH("Este mês", SpendPeriod.CURRENT_MONTH),
    SIX("6 meses", SpendPeriod.LAST_SIX_MONTHS),
    YEAR("Este ano", SpendPeriod.CURRENT_YEAR),
    ALL("Tudo", SpendPeriod.ALL),
    CUSTOM("Personalizado", SpendPeriod.CUSTOM)
}

private enum class Grouping(val label: String, val kind: SpendGrouping) {
    MONTH("Mês", SpendGrouping.MONTH), YEAR("Ano", SpendGrouping.YEAR), KM("KM", SpendGrouping.KM)
}

/** Barra do gráfico: valores do domínio + rótulos para a tela. */
private data class Bucket(val label: String, val fullLabel: String, val maintenance: Double, val fuel: Double) {
    val total get() = maintenance + fuel
}

private data class Slice(val label: String, val value: Double, val color: Color)

private val SLICE_COLORS = listOf(
    Color(0xFF1569CC), Color(0xFF0E2A57), Color(0xFF74B8F0),
    Color(0xFF5C7CA6), Color(0xFFA9C4E4), Color(0xFFC9D6E8)
)

private fun niceCeil(v: Double): Double {
    if (v <= 0) return 1.0
    val p = 10.0.pow(floor(log10(v)))
    val n = v / p
    val nice = when {
        n <= 1 -> 1.0
        n <= 2 -> 2.0
        n <= 2.5 -> 2.5
        n <= 5 -> 5.0
        else -> 10.0
    }
    return nice * p
}

private fun compactMoney(v: Double): String = when {
    v >= 1_000_000 -> String.format(PT_BR, "R$ %.1f mi", v / 1_000_000)
    v >= 1_000 -> String.format(PT_BR, "R$ %.1f mil", v / 1_000)
    else -> String.format(PT_BR, "R$ %.0f", v)
}

/** Converte os grupos calculados no domínio em barras com rótulos. */
private fun labelBuckets(raw: List<SpendBucket>, grouping: Grouping, start: Long, end: Long): List<Bucket> {
    val multiYear = SpendingAnalytics.yearOf(start) != SpendingAnalytics.yearOf(end)
    val shortFmt = SimpleDateFormat(if (multiYear) "MMM yy" else "MMM", PT_BR)
    val longFmt = SimpleDateFormat("MMMM 'de' yyyy", PT_BR)
    return raw.map { b ->
        when (grouping) {
            Grouping.MONTH -> Bucket(
                shortFmt.format(Date(b.start)).replace(".", "").replaceFirstChar { it.titlecase(PT_BR) },
                longFmt.format(Date(b.start)).replaceFirstChar { it.titlecase(PT_BR) },
                b.maintenance, b.fuel
            )
            Grouping.YEAR -> SpendingAnalytics.yearOf(b.start).let { y -> Bucket("$y", "Ano de $y", b.maintenance, b.fuel) }
            Grouping.KM -> Bucket(
                if (b.end - b.start >= 1_000) "${b.start / 1000}k" else formatKm(b.start),
                "De ${formatKm(b.start)} a ${formatKm(b.end)} km",
                b.maintenance, b.fuel
            )
        }
    }
}

private fun pctText(delta: Double): String =
    (if (delta >= 0) "▲ " else "▼ ") + String.format(PT_BR, "%.0f%%", abs(delta))

// ─── Tela ────────────────────────────────────────────────────────────────────

/**
 * RF-08: painel financeiro com filtros por veículo e período, indicadores com variação em
 * relação ao período anterior, gráfico de barras interativo (por mês, ano ou quilometragem),
 * gráfico de rosca por categoria com tabela, resumo e últimos lançamentos.
 * Usa sempre a data do serviço ou do abastecimento. Os cálculos ficam em domain/SpendingAnalytics.
 */
@Composable
fun DashboardScreen(
    userId: Long,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as AutoTechApplication
    var vehicles by remember { mutableStateOf<List<VehicleEntity>?>(null) }
    var entries by remember { mutableStateOf<List<Expense>>(emptyList()) }

    LaunchedEffect(userId) {
        // inclui veículos excluídos que mantiveram o histórico (RF-03)
        val vs = app.graph.vehicleRepository.listAllVehicles(userId)
        entries = vs.flatMap { v ->
            app.graph.maintenanceRepository.listForPdf(v.id).map { it.toExpense() } +
                app.graph.fuelRepository.listForVehicle(v.id).map { it.toExpense() }
        }
        vehicles = vs
    }

    var vehicleFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var period by rememberSaveable { mutableStateOf(Period.SIX) }
    var grouping by rememberSaveable { mutableStateOf(Grouping.MONTH) }
    var customStart by rememberSaveable { mutableLongStateOf(SpendingAnalytics.monthStart(System.currentTimeMillis(), 2)) }
    var customEnd by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showCustom by remember { mutableStateOf(false) }

    val vs = vehicles
    // por quilometragem só faz sentido com um veículo (cada carro tem o próprio odômetro)
    val singleVehicle = vehicleFilter != null || (vs?.size ?: 0) == 1
    val effGrouping = if (grouping == Grouping.KM && !singleVehicle) Grouping.MONTH else grouping

    val byVehicle = remember(entries, vehicleFilter) {
        entries.filter { vehicleFilter == null || it.vehicleId == vehicleFilter }
    }
    val range = remember(entries, period, customStart, customEnd) {
        SpendingAnalytics.range(period.kind, entries, customStart, customEnd)
    }
    val start = range.first
    val end = range.second
    val filtered = remember(byVehicle, range) { byVehicle.filter { it.date in start..end } }
    val previous = remember(byVehicle, range) {
        val (ps, pe) = SpendingAnalytics.previousRange(start, end)
        byVehicle.filter { it.date in ps..pe }
    }
    val buckets = remember(filtered, effGrouping, range) {
        labelBuckets(SpendingAnalytics.buckets(filtered, effGrouping.kind, start, end), effGrouping, start, end)
    }

    AtScreen {
        AtHeader("Dashboard", "Gastos por período e quilometragem", onBack = onBack)

        when {
            vs == null -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = AtColors.Primary) }

            entries.isEmpty() -> AtCard(Modifier.fillMaxWidth()) {
                AtListRow(
                    title = "Sem lançamentos ainda",
                    subtitle = "Registre manutenções e abastecimentos com valor para ver o painel",
                    icon = Icons.Rounded.Insights
                )
            }

            else -> {
                // ── Filtros ──────────────────────────────────────────────────
                Reveal(0) {
                    Column {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AtChip("Todos os veículos", vehicleFilter == null, { vehicleFilter = null }, icon = Icons.Rounded.DirectionsCar)
                            vs.forEach { v ->
                                AtChip("${v.shortName} · ${v.plate}" + if (v.archived) " (excluído)" else "", vehicleFilter == v.id, { vehicleFilter = v.id })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val df = SimpleDateFormat("dd/MM/yy", PT_BR)
                            Period.entries.forEach { p ->
                                val label = if (p == Period.CUSTOM && period == Period.CUSTOM)
                                    "${df.format(Date(customStart))} – ${df.format(Date(customEnd))}" else p.label
                                AtChip(
                                    label, period == p,
                                    { if (p == Period.CUSTOM) showCustom = true else period = p },
                                    icon = if (p == Period.CUSTOM) Icons.Rounded.CalendarMonth else null
                                )
                            }
                        }
                    }
                }

                // ── Indicadores (cartões do topo) ────────────────────────────
                Reveal(1) {
                    val total = filtered.sumOf { it.amount }
                    val maint = filtered.filter { !it.isFuel }.sumOf { it.amount }
                    val fuel = filtered.filter { it.isFuel }.sumOf { it.amount }
                    val pTotal = previous.sumOf { it.amount }
                    val pMaint = previous.filter { !it.isFuel }.sumOf { it.amount }
                    val pFuel = previous.filter { it.isFuel }.sumOf { it.amount }
                    val km = SpendingAnalytics.kmDriven(filtered)
                    val literPrice = SpendingAnalytics.averageLiterPrice(filtered)
                    val paidFills = filtered.count { it.isFuel && it.amount > 0 && it.liters > 0 }

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(Icons.Rounded.Payments, "Total gasto", formatMoney(total), SpendingAnalytics.variation(total, pTotal))
                        KpiCard(Icons.Rounded.Build, "Manutenção", formatMoney(maint), SpendingAnalytics.variation(maint, pMaint))
                        KpiCard(Icons.Rounded.LocalGasStation, "Combustível", formatMoney(fuel), SpendingAnalytics.variation(fuel, pFuel))
                        KpiCard(
                            Icons.Rounded.Route, "Custo por km",
                            if (km > 0) String.format(PT_BR, "R$ %.2f", total / km) else "—",
                            null, footnote = if (km > 0) "${formatKm(km)} km rodados" else "Precisa de 2 lançamentos"
                        )
                        KpiCard(
                            Icons.Rounded.WaterDrop, "Preço médio do litro",
                            literPrice?.let { String.format(PT_BR, "R$ %.2f", it) } ?: "—",
                            null, footnote = "$paidFills abastecimento(s)"
                        )
                    }
                }

                // ── Gráfico de barras ────────────────────────────────────────
                Reveal(2) {
                    AtCard(Modifier.fillMaxWidth()) {
                        AtCardTitle(
                            when (effGrouping) {
                                Grouping.MONTH -> "Gastos por mês"
                                Grouping.YEAR -> "Gastos por ano"
                                Grouping.KM -> "Gastos por quilometragem"
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Grouping.entries.forEach { g ->
                                val enabled = g != Grouping.KM || singleVehicle
                                AtChip(g.label, effGrouping == g, { if (enabled) grouping = g })
                            }
                        }
                        if (!singleVehicle) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Escolha um veículo para ver por quilometragem.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AtColors.Muted
                            )
                        }
                        Spacer(Modifier.height(14.dp))

                        if (buckets.isEmpty() || buckets.all { it.total == 0.0 }) {
                            Text(
                                "Nenhum gasto neste período.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AtColors.Muted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            var selected by remember(buckets) {
                                mutableIntStateOf(buckets.indexOfLast { it.total > 0 }.coerceAtLeast(0))
                            }
                            val idx = selected.coerceIn(0, buckets.lastIndex)
                            // "balão" do item selecionado (como o "Revenue $6.4M" da referência)
                            // O conteúdo animado recebe os dados da barra (e não a posição): ao trocar o
                            // período a lista muda de tamanho, e ler a barra antiga pela posição travava o app.
                            AnimatedContent(
                                targetState = buckets[idx] to buckets.getOrNull(idx - 1),
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                                label = "barInfo"
                            ) { (sel, prevB) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AtColors.Field)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        AtLabel(sel.fullLabel)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(formatMoney(sel.total), style = MaterialTheme.typography.titleLarge)
                                            if (prevB != null && prevB.total > 0) {
                                                val d = (sel.total - prevB.total) / prevB.total * 100
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    pctText(d),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (d > 0) AtColors.Danger else AtColors.Primary
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Manut. ${formatMoney(sel.maintenance)}", style = MaterialTheme.typography.labelMedium, color = AtColors.Muted)
                                        Text("Comb. ${formatMoney(sel.fuel)}", style = MaterialTheme.typography.labelMedium, color = AtColors.Muted)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            BarChart(buckets, idx) { selected = it }
                        }
                    }
                }

                // ── Rosca por categoria + tabela ─────────────────────────────
                Reveal(3) {
                    val cats = SpendingAnalytics.byCategory(filtered)
                    val top = cats.take(5).toMutableList()
                    val rest = cats.drop(5).sumOf { it.second }
                    if (rest > 0) top += "Outros" to rest
                    val slices = top.mapIndexed { i, (k, v) -> Slice(k, v, SLICE_COLORS[i % SLICE_COLORS.size]) }
                    val sum = slices.sumOf { it.value }

                    AtCard(Modifier.fillMaxWidth()) {
                        AtCardTitle("Distribuição por categoria", "Para onde foi o dinheiro no período")
                        Spacer(Modifier.height(16.dp))
                        if (slices.isEmpty()) {
                            Text("Nenhum gasto neste período.", style = MaterialTheme.typography.bodyMedium, color = AtColors.Muted)
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Donut(slices, "Maior gasto", slices.first().label)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text("Categoria", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                Text("%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                                Text("Valor", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(104.dp), textAlign = TextAlign.End)
                            }
                            Spacer(Modifier.height(6.dp))
                            slices.forEach { s ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .background(s.color, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        s.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        String.format(PT_BR, "%.0f%%", s.value / sum * 100),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AtColors.Muted,
                                        modifier = Modifier.width(52.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        formatMoney(s.value),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(104.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Resumo rápido ("Quick Stats") ────────────────────────────
                Reveal(4) {
                    val fills = filtered.filter { it.isFuel }
                    val maints = filtered.filter { !it.isFuel }
                    val dfl = SimpleDateFormat("dd/MM/yyyy", PT_BR)
                    val km = SpendingAnalytics.kmDriven(filtered)
                    AtCard(Modifier.fillMaxWidth()) {
                        AtCardTitle("Resumo rápido")
                        Spacer(Modifier.height(10.dp))
                        StatRow("Abastecimentos", "${fills.size}")
                        StatRow("Litros abastecidos", String.format(PT_BR, "%.1f L", fills.sumOf { it.liters }))
                        StatRow("Manutenções", "${maints.size}")
                        StatRow("Gasto médio por manutenção", if (maints.isNotEmpty()) formatMoney(maints.sumOf { it.amount } / maints.size) else "—")
                        StatRow("Km rodados no período", if (km > 0) "${formatKm(km)} km" else "—")
                        StatRow("Último lançamento", filtered.maxOfOrNull { it.date }?.let { dfl.format(Date(it)) } ?: "—")
                    }
                }

                // ── Últimos lançamentos ──────────────────────────────────────
                Reveal(5) {
                    val dfl = SimpleDateFormat("dd/MM/yyyy", PT_BR)
                    val names = vs.associate { it.id to it.shortName }
                    AtCard(Modifier.fillMaxWidth()) {
                        AtCardTitle("Últimos lançamentos", "No período selecionado")
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val recent = filtered.sortedByDescending { it.date }.take(5)
                            if (recent.isEmpty()) {
                                Text("Nenhum lançamento neste período.", style = MaterialTheme.typography.bodyMedium, color = AtColors.Muted)
                            }
                            recent.forEach { e ->
                                AtListRow(
                                    title = if (e.isFuel) e.category else com.autotech.android.domain.MaintenanceCatalog.label(e.itemKey ?: "other"),
                                    subtitle = "${names[e.vehicleId] ?: ""} · ${dfl.format(Date(e.date))} · ${formatKm(e.km)} km",
                                    icon = if (e.isFuel) Icons.Rounded.LocalGasStation else maintenanceIcon(e.itemKey ?: "other"),
                                    trailing = { Text(formatMoney(e.amount), style = MaterialTheme.typography.titleSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showCustom) {
        var s by remember { mutableLongStateOf(customStart) }
        var e by remember { mutableLongStateOf(customEnd) }
        AlertDialog(
            onDismissRequest = { showCustom = false },
            containerColor = AtColors.Card,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Período personalizado", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtDateField("Início", s, { s = it }, maxMillis = e)
                    AtDateField("Fim", e, { e = it }, minMillis = s)
                }
            },
            confirmButton = {
                AtPillButton("Aplicar", {
                    customStart = s
                    customEnd = e
                    period = Period.CUSTOM
                    showCustom = false
                }, compact = true)
            },
            dismissButton = {
                AtPillButton("Cancelar", { showCustom = false }, style = AtButtonStyle.Soft, compact = true)
            }
        )
    }
}

// ─── Componentes do painel ───────────────────────────────────────────────────

@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    delta: Double?,
    footnote: String = "Sem período anterior"
) {
    AtCard(Modifier.width(178.dp), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(AtColors.AccentSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AtColors.Primary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(Modifier.height(4.dp))
        if (delta != null) {
            // gasto maior que no período anterior = vermelho; menor = azul
            Row {
                Text(pctText(delta), style = MaterialTheme.typography.labelMedium, color = if (delta > 0) AtColors.Danger else AtColors.Primary)
                Text(" vs anterior", style = MaterialTheme.typography.labelMedium, color = AtColors.Muted)
            }
        } else {
            Text(footnote, style = MaterialTheme.typography.labelMedium, color = AtColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val CHART_H = 170.dp

@Composable
private fun BarChart(buckets: List<Bucket>, selected: Int, onSelect: (Int) -> Unit) {
    val top = niceCeil(buckets.maxOf { it.total })
    val scroll = rememberScrollState()
    LaunchedEffect(buckets.size) { scroll.animateScrollTo(scroll.maxValue) }
    Row(Modifier.fillMaxWidth()) {
        // eixo Y
        Column(
            Modifier
                .height(CHART_H)
                .width(58.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 3 downTo 0) {
                Text(compactMoney(top * i / 3), style = MaterialTheme.typography.labelSmall, color = AtColors.Muted, maxLines = 1)
            }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val n = buckets.size
            val gap = 8.dp
            val barW: Dp = maxOf(26.dp, (maxWidth - gap * (n - 1)) / n).coerceAtMost(54.dp)
            // linhas de grade tracejadas
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(CHART_H)
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                for (i in 0..3) {
                    val y = size.height * i / 3f
                    drawLine(AtColors.Line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
            Row(
                Modifier.horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                buckets.forEachIndexed { i, b ->
                    val isSel = i == selected
                    val h by animateDpAsState(
                        CHART_H * (b.total / top).toFloat().coerceIn(0f, 1f),
                        tween(700, easing = FastOutSlowInEasing),
                        label = "bar"
                    )
                    Column(
                        Modifier
                            .width(barW)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.height(CHART_H), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(maxOf(h, 3.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        if (isSel) Brush.verticalGradient(listOf(AtColors.Primary, AtColors.Accent))
                                        else Brush.verticalGradient(listOf(AtColors.AccentLight, AtColors.AccentSoft))
                                    )
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            b.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSel) AtColors.Primary else AtColors.Muted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Donut(slices: List<Slice>, centerTop: String, centerMain: String) {
    val progress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val sum = slices.sumOf { it.value }.toFloat()
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(210.dp)) {
            val sw = 30.dp.toPx()
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            var startAngle = -90f
            val gap = if (slices.size > 1) 2f else 0f
            slices.forEach { s ->
                val sweep = 360f * (s.value.toFloat() / sum) * progress.value
                if (sweep > gap) {
                    drawArc(
                        color = s.color,
                        startAngle = startAngle,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        topLeft = tl,
                        size = Size(d, d),
                        style = Stroke(sw)
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
            Text(centerTop, style = MaterialTheme.typography.labelMedium, color = AtColors.Muted)
            Text(
                centerMain,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AtColors.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
