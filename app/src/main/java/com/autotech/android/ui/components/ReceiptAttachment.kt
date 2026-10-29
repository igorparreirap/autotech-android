package com.autotech.android.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.PendingReceipt
import com.autotech.android.util.ReceiptFileStore
import kotlinx.coroutines.launch

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
 * Anexo de comprovante: aceita qualquer tipo de arquivo. Mostra o nome original e
 * o nome padronizado com que o arquivo será guardado ao salvar o registro.
 */
@Composable
fun ReceiptAttachment(
    pending: PendingReceipt?,
    onChange: (PendingReceipt?) -> Unit,
    finalBaseName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                val staged = ReceiptFileStore.stage(context, uri)
                loading = false
                if (staged == null) {
                    Toast.makeText(context, "Não foi possível anexar esse arquivo.", Toast.LENGTH_SHORT).show()
                } else {
                    ReceiptFileStore.discard(pending)
                    onChange(staged)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pending != null) {
            AtListRow(
                title = pending.originalName,
                subtitle = "Será salvo como $finalBaseName.${pending.extension}",
                subtitleLines = 2,
                icon = Icons.Rounded.Description,
                iconBg = AtColors.AccentSoft,
                iconTint = AtColors.Primary,
                trailing = {
                    IconButton(onClick = {
                        ReceiptFileStore.discard(pending)
                        onChange(null)
                    }) { Icon(Icons.Rounded.Close, "Remover comprovante", tint = AtColors.Muted) }
                }
            )
        }
        AtPillButton(
            if (pending == null) "Anexar comprovante" else "Trocar arquivo",
            // qualquer tipo de arquivo: foto, PDF, XML da nota fiscal...
            { picker.launch("*/*") },
            Modifier.fillMaxWidth(),
            style = AtButtonStyle.Soft,
            leadingIcon = if (pending == null) Icons.Rounded.AttachFile else Icons.Rounded.SwapHoriz,
            loading = loading
        )
        if (pending == null) {
            Text(
                "Qualquer arquivo: foto, PDF, XML da nota fiscal...",
                style = MaterialTheme.typography.bodySmall,
                color = AtColors.Muted
            )
        }
    }
}
