package com.autotech.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autotech.android.AutoTechApplication
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCheckRow
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.shortName
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.VehicleReportPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RF-10: janela do relatório em PDF (tela inicial e tela do veículo).
 * Existe um relatório por veículo: se nada mudou desde o último, ele é aberto; se algo mudou
 * (registros, dados do veículo, foto, comprovantes ou opções), um novo é gerado e o antigo, apagado.
 */
@Composable
fun VehicleReportDialog(
    vehicle: VehicleEntity,
    onDismiss: () -> Unit,
    onGenerated: (File) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val scope = rememberCoroutineScope()
    val last = remember(vehicle.id) { VehicleReportPdf.lastOptions(context, vehicle.id) }
    var includeFuel by remember { mutableStateOf(last.includeFuel) }
    var includeReceipts by remember { mutableStateOf(last.includeReceipts) }
    var maintenance by remember { mutableStateOf<List<MaintenanceRecordEntity>?>(null) }
    var fuel by remember { mutableStateOf<List<FuelRecordEntity>>(emptyList()) }
    var upToDate by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val current = remember(vehicle.id) { VehicleReportPdf.currentReport(context, vehicle.id) }

    LaunchedEffect(vehicle.id) {
        fuel = app.graph.fuelRepository.listForVehicle(vehicle.id)
        maintenance = app.graph.maintenanceRepository.listForPdf(vehicle.id)
    }
    LaunchedEffect(maintenance, includeFuel, includeReceipts) {
        val m = maintenance ?: return@LaunchedEffect
        upToDate = withContext(Dispatchers.IO) {
            val sig = VehicleReportPdf.signature(context, vehicle, m, fuel, VehicleReportPdf.Options(includeFuel, includeReceipts))
            VehicleReportPdf.isUpToDate(context, vehicle.id, sig)
        }
    }

    fun open() {
        val m = maintenance ?: return
        scope.launch {
            working = true
            error = null
            VehicleReportPdf.ensure(context, vehicle, m, if (includeFuel) fuel else emptyList(), VehicleReportPdf.Options(includeFuel, includeReceipts))
                .onSuccess { onGenerated(it) }
                .onFailure { error = it.message ?: "Falha ao gerar o PDF." }
            working = false
        }
    }

    val m = maintenance
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = AtColors.Card,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Relatório do ${vehicle.shortName}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "O PDF traz os dados do veículo, o odômetro e todas as manutenções" +
                        (m?.let { " (${it.size})" } ?: "") + ". Escolha o que mais incluir:",
                    style = MaterialTheme.typography.bodyMedium
                )
                AtCheckRow("Abastecimentos (${fuel.size})", includeFuel) { includeFuel = it }
                val receipts = (m?.count { !it.receiptPath.isNullOrBlank() } ?: 0) +
                    if (includeFuel) fuel.count { !it.receiptPath.isNullOrBlank() } else 0
                AtCheckRow("Comprovantes anexados ($receipts)", includeReceipts) { includeReceipts = it }
                if (includeReceipts) {
                    Text(
                        "Fotos e PDFs dos comprovantes entram como páginas no final do relatório.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                }
                current?.let { f ->
                    val whenText = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(f.lastModified()))
                    Text(
                        if (upToDate) "O relatório de $whenText está atualizado e será aberto."
                        else "Houve mudanças desde o relatório de $whenText: um novo será gerado e o anterior, apagado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                }
                error?.let { AtMessage(it, isError = true) }
            }
        },
        confirmButton = {
            AtPillButton(
                if (upToDate) "Abrir PDF" else "Gerar PDF",
                { open() },
                compact = true,
                loading = working,
                enabled = m != null
            )
        },
        dismissButton = {
            AtPillButton("Cancelar", onDismiss, style = AtButtonStyle.Soft, compact = true, enabled = !working)
        }
    )
}
