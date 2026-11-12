package com.autotech.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotech.android.domain.FuelType
import com.autotech.android.domain.MaintenanceCatalog
import com.autotech.android.ui.theme.AtColors

/**
 * RF-04: escolha do item de manutenção em duas etapas — primeiro a parte do carro (motor, freios,
 * suspensão...), depois o item: os principais primeiro, em seguida todos os itens comuns da parte,
 * os itens próprios que o usuário já criou e, por último, "Outro" para especificar um serviço novo.
 * Também há uma busca pelo nome em todas as partes. Itens que não valem para o combustível do
 * veículo (ex.: velas de ignição num diesel) não aparecem.
 *
 * Seleção múltipla: cada toque marca ou desmarca o item, em qualquer parte do carro, para registrar
 * de uma vez as peças e os serviços de um mesmo comprovante. O primeiro marcado é o registro principal.
 * A revisão é exclusiva: ela tem a própria lista de itens verificados.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceItemPicker(
    selectedKeys: List<String>,
    onChange: (List<String>) -> Unit,
    fuel: FuelType?,
    customKeys: List<String>,
    draftKey: String? = null,
    onDraftChange: (String?) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(selectedKeys.firstOrNull()?.let { MaintenanceCatalog.categoryOf(it) }) }
    var otherMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    // item próprio sendo digitado agora ("Outro"): só entra na seleção quando o usuário toca em
    // "Adicionar", para que cada letra digitada não vire um item separado
    val customCurrent = draftKey

    /** Confirma o item digitado: ele entra na seleção e o campo fica livre para outro. */
    fun commitDraft(base: List<String> = selectedKeys): List<String> {
        val d = draftKey
        customName = ""
        onDraftChange(null)
        return if (d != null && d !in base) base.filter { it !in MaintenanceCatalog.REVIEW_KEYS } + d else base
    }

    // item vindo de fora (ex.: completar uma manutenção marcada como "Já fiz")
    LaunchedEffect(selectedKeys.firstOrNull()) {
        val key = selectedKeys.firstOrNull() ?: return@LaunchedEffect
        if (category == null) category = MaintenanceCatalog.categoryOf(key)
    }

    fun isOn(key: String) = key in selectedKeys

    /** Marca ou desmarca; a revisão não se mistura com outros itens. */
    fun toggle(key: String) {
        category = MaintenanceCatalog.categoryOf(key) ?: category
        val review = MaintenanceCatalog.REVIEW_KEYS
        val current = selectedKeys
        onChange(
            when {
                key in current -> current - key
                key in review -> listOf(key)
                current.any { it in review } -> listOf(key)
                else -> current + key
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val shown = selectedKeys
        if (shown.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AtColors.AccentSoft, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (shown.size > 1) {
                    Text("${shown.size} itens selecionados", style = MaterialTheme.typography.labelMedium, color = AtColors.Primary)
                }
                shown.forEachIndexed { i, key ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = AtColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${MaintenanceCatalog.categoryLabel(key)} › ${MaintenanceCatalog.label(key)}" + if (i == 0 && shown.size > 1) " (principal)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            color = AtColors.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Rounded.Close, "Remover",
                            tint = AtColors.Primary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onChange(selectedKeys - key) }
                        )
                    }
                }
                if (shown.none { it in MaintenanceCatalog.REVIEW_KEYS }) {
                    Text(
                        "Toque em outros itens para registrar tudo o que veio no mesmo comprovante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                }
            }
        }

        AtField(query, { query = it }, "Buscar item", placeholder = "Ex.: vela, pastilha, amortecedor", leadingIcon = Icons.Rounded.Search)

        if (query.isNotBlank()) {
            val found = MaintenanceCatalog.search(query, fuel)
            val foundCustom = customKeys.filter { MaintenanceCatalog.label(it).contains(query.trim(), ignoreCase = true) }
            if (found.isEmpty() && foundCustom.isEmpty()) {
                Text(
                    "Nenhum item com esse nome. Escolha a parte do carro abaixo e use “Outro” para especificar o serviço.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AtColors.Muted
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    found.forEach { item ->
                        AtChip(item.label, selected = isOn(item.key), onClick = { toggle(item.key); query = "" }, icon = maintenanceIcon(item.key))
                    }
                    foundCustom.forEach { key ->
                        AtChip(MaintenanceCatalog.label(key), selected = isOn(key), onClick = { toggle(key); query = "" }, icon = maintenanceIcon(key))
                    }
                }
            }
        }

        AtLabel("Parte do carro")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MaintenanceCatalog.categories.forEach { c ->
                AtChip(
                    c.label,
                    selected = category == c.key,
                    onClick = {
                        if (category != c.key) {
                            category = c.key
                            // o nome digitado continua no campo e passa a valer para a nova parte do carro
                            if (otherMode) onDraftChange(MaintenanceCatalog.customKey(c.key, customName, customKeys))
                        }
                    },
                    icon = categoryIcon(c.key)
                )
            }
        }

        category?.let { cat ->
            val catLabel = MaintenanceCatalog.categories.firstOrNull { it.key == cat }?.label.orEmpty()
            AtLabel("Principais")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MaintenanceCatalog.mainItems(cat, fuel).forEach { item ->
                    AtChip(item.label, selected = isOn(item.key), onClick = { toggle(item.key) })
                }
            }
            val others = MaintenanceCatalog.otherItems(cat, fuel)
            if (others.isNotEmpty()) {
                AtLabel("Todos os itens de $catLabel")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { item ->
                        AtChip(item.label, selected = isOn(item.key), onClick = { toggle(item.key) })
                    }
                }
            }
            val mine = customKeys.filter { MaintenanceCatalog.categoryOf(it) == cat }
            if (mine.isNotEmpty()) {
                AtLabel("Seus itens")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mine.forEach { key ->
                        AtChip(MaintenanceCatalog.label(key), selected = isOn(key), onClick = { toggle(key) })
                    }
                }
            }
            AtChip(
                "Outro (especificar)",
                selected = otherMode,
                onClick = {
                    otherMode = true
                    // "Outro" não se mistura com a revisão
                    if (selectedKeys.any { it in MaintenanceCatalog.REVIEW_KEYS }) onChange(selectedKeys.filter { it !in MaintenanceCatalog.REVIEW_KEYS })
                },
                icon = Icons.Rounded.Add
            )
            if (otherMode) {
                AtField(
                    customName,
                    { v ->
                        customName = v.take(40)
                        onDraftChange(MaintenanceCatalog.customKey(cat, customName, customKeys))
                    },
                    "Qual foi o serviço?",
                    placeholder = "Ex.: selo do motor"
                )
                // vários itens fora da lista no mesmo atendimento: confirma o nome digitado e abre outro
                if (customCurrent != null) {
                    AtPillButton(
                        "Adicionar \u201c${MaintenanceCatalog.label(customCurrent)}\u201d",
                        { onChange(commitDraft()) },
                        style = AtButtonStyle.Accent,
                        compact = true,
                        icon = Icons.Rounded.Add
                    )
                    Text(
                        "Toque em Adicionar para incluir o item. Depois, se precisar, digite outro no mesmo campo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AtColors.Muted
                    )
                }
            }
        }
    }
}
