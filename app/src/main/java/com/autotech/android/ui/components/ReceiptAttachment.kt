package com.autotech.android.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autotech.android.domain.ReceiptPaths
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.PendingReceipt
import com.autotech.android.util.ReceiptFileStore
import kotlinx.coroutines.launch
import java.io.File

/** Linha com caixa de seleção (ex.: "Mesmo KM para o serviço e o registro"). */
@Composable
fun AtCheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = AtColors.Primary,
                uncheckedColor = AtColors.Muted,
                checkmarkColor = Color.White
            )
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * RF-04 / RF-05: anexos de comprovante. Aceita qualquer tipo de arquivo e vários de uma vez
 * (fotos da bomba e do painel, nota fiscal...). Mostra o nome original de cada um e o nome
 * padronizado com que será guardado ao salvar o registro.
 */
@Composable
fun ReceiptAttachments(
    pending: List<PendingReceipt>,
    onChange: (List<PendingReceipt>) -> Unit,
    finalBaseName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                loading = true
                val staged = uris.mapNotNull { ReceiptFileStore.stage(context, it) }
                loading = false
                if (staged.size < uris.size) {
                    Toast.makeText(context, "Alguns arquivos não puderam ser anexados.", Toast.LENGTH_SHORT).show()
                }
                if (staged.isNotEmpty()) onChange(pending + staged)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pending.forEachIndexed { i, p ->
            val suffix = if (i == 0) "" else "_${i + 1}"
            AtListRow(
                title = p.originalName,
                subtitle = "Será salvo como $finalBaseName$suffix.${p.extension}",
                subtitleLines = 2,
                icon = Icons.Rounded.Description,
                iconBg = AtColors.AccentSoft,
                iconTint = AtColors.Primary,
                trailing = {
                    IconButton(onClick = {
                        ReceiptFileStore.discard(p)
                        onChange(pending - p)
                    }) { Icon(Icons.Rounded.Close, "Remover comprovante", tint = AtColors.Muted) }
                }
            )
        }
        AtPillButton(
            if (pending.isEmpty()) "Anexar comprovantes" else "Anexar mais arquivos",
            // qualquer tipo de arquivo: foto, PDF, XML da nota fiscal...
            { picker.launch("*/*") },
            Modifier.fillMaxWidth(),
            style = AtButtonStyle.Soft,
            leadingIcon = Icons.Rounded.AttachFile,
            loading = loading
        )
        Text(
            if (pending.isEmpty()) "Pode escolher vários arquivos de uma vez: fotos, PDF, XML da nota fiscal..."
            else "${pending.size} ${if (pending.size == 1) "arquivo anexado" else "arquivos anexados"}",
            style = MaterialTheme.typography.bodySmall,
            color = AtColors.Muted
        )
    }
}

/**
 * Botão de comprovante nas listas de histórico: com um arquivo, abre direto; com vários,
 * mostra a lista para o usuário escolher qual abrir.
 */
@Composable
fun ReceiptOpenButton(raw: String?) {
    val paths = ReceiptPaths.split(raw)
    if (paths.isEmpty()) return
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { if (paths.size == 1) ReceiptFileStore.open(context, paths[0]) else open = true }) {
            BadgedBox(badge = { if (paths.size > 1) Badge { Text("${paths.size}") } }) {
                Icon(Icons.Rounded.AttachFile, "Abrir comprovante", tint = AtColors.Primary)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            paths.forEach { path ->
                DropdownMenuItem(
                    text = { Text(File(path).name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Rounded.Description, null, tint = AtColors.Primary) },
                    onClick = {
                        open = false
                        ReceiptFileStore.open(context, path)
                    }
                )
            }
        }
    }
}
