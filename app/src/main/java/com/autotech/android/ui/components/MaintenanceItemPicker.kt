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
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceItemPicker(
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    fuel: FuelType?,
    customKeys: List<String>
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(selectedKey?.let { MaintenanceCatalog.categoryOf(it) }) }
    var otherMode by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }

    // item vindo de fora (ex.: completar uma manutenção marcada como "Já fiz")
    LaunchedEffect(selectedKey) {
        val key = selectedKey ?: return@LaunchedEffect
        if (category == null) category = MaintenanceCatalog.categoryOf(key)
    }

    fun pick(key: String) {
        otherMode = false
        category = MaintenanceCatalog.categoryOf(key) ?: category
        onSelect(key)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        selectedKey?.let { key ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(AtColors.AccentSoft, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = AtColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${MaintenanceCatalog.categoryLabel(key)} › ${MaintenanceCatalog.label(key)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = AtColors.Primary
                )
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
                        AtChip(item.label, selected = selectedKey == item.key, onClick = { pick(item.key); query = "" }, icon = maintenanceIcon(item.key))
                    }
                    foundCustom.forEach { key ->
                        AtChip(MaintenanceCatalog.label(key), selected = selectedKey == key, onClick = { pick(key); query = "" }, icon = maintenanceIcon(key))
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
                            otherMode = false
                            if (selectedKey?.let { MaintenanceCatalog.categoryOf(it) } != c.key) onSelect(null)
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
                    AtChip(item.label, selected = !otherMode && selectedKey == item.key, onClick = { pick(item.key) })
                }
            }
            val others = MaintenanceCatalog.otherItems(cat, fuel)
            if (others.isNotEmpty()) {
                AtLabel("Todos os itens de $catLabel")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { item ->
                        AtChip(item.label, selected = !otherMode && selectedKey == item.key, onClick = { pick(item.key) })
                    }
                }
            }
            val mine = customKeys.filter { MaintenanceCatalog.categoryOf(it) == cat }
            if (mine.isNotEmpty()) {
                AtLabel("Seus itens")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mine.forEach { key ->
                        AtChip(MaintenanceCatalog.label(key), selected = !otherMode && selectedKey == key, onClick = { pick(key) })
                    }
                }
            }
            AtChip(
                "Outro (especificar)",
                selected = otherMode,
                onClick = {
                    otherMode = true
                    onSelect(MaintenanceCatalog.customKey(cat, customName, customKeys))
                },
                icon = Icons.Rounded.Add
            )
            if (otherMode) {
                AtField(
                    customName,
                    { v ->
                        customName = v.take(40)
                        onSelect(MaintenanceCatalog.customKey(cat, customName, customKeys))
                    },
                    "Qual foi o serviço?",
                    placeholder = "Ex.: selo do motor"
                )
            }
        }
    }
}
