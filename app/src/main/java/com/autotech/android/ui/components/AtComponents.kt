package com.autotech.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CarRepair
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Window
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotech.android.ui.theme.AtColors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Formatação ──────────────────────────────────────────────────────────────

val PT_BR: Locale = Locale("pt", "BR")

fun formatKm(km: Long): String = NumberFormat.getIntegerInstance(PT_BR).format(km)

fun formatMoney(value: Double): String = NumberFormat.getCurrencyInstance(PT_BR).format(value)

/** Aceita "12,50" e "12.50". */
fun parseDecimal(raw: String): Double? = raw.trim().replace(',', '.').toDoubleOrNull()

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase(PT_BR)
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase(PT_BR)
    }
}

fun maintenanceIcon(itemKey: String): ImageVector =
    if (itemKey == "oil") Icons.Rounded.OilBarrel
    else categoryIcon(com.autotech.android.domain.MaintenanceCatalog.categoryOf(itemKey))

/** Ícone de cada parte do carro (RF-04). */
fun categoryIcon(category: String?): ImageVector = when (category) {
    "motor" -> Icons.Rounded.Settings
    "cambio" -> Icons.Rounded.Tune
    "freios" -> Icons.Rounded.DonutLarge
    "suspensao" -> Icons.Rounded.Compress
    "pneus" -> Icons.Rounded.TireRepair
    "eletrica" -> Icons.Rounded.BatteryChargingFull
    "ar" -> Icons.Rounded.AcUnit
    "funilaria" -> Icons.Rounded.FormatPaint
    "vidros" -> Icons.Rounded.Window
    "revisao" -> Icons.Rounded.CarRepair
    else -> Icons.Rounded.Build
}

// ─── Estrutura ───────────────────────────────────────────────────────────────

/** Tela padrão: fundo cinza, respeita barras do sistema/teclado e rola verticalmente. */
@Composable
fun AtScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxSize()
        .background(AtColors.Background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
    val scrolled = if (scrollable) base.verticalScroll(rememberScrollState()) else base
    Column(
        scrolled.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/** Entrada suave (fade + subida) com atraso escalonado pelo índice. */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(45L * index)
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380)) + slideInVertically(tween(460, easing = FastOutSlowInEasing)) { it / 5 }
    ) { content() }
}

@Composable
private fun pressScale(interaction: MutableInteractionSource): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    return scale
}

/** Cartão branco com cantos grandes, como os painéis do layout de referência. */
@Composable
fun AtCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = AtColors.Card,
    contentColor: Color = AtColors.Ink,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        val scale = pressScale(interaction)
        Surface(
            onClick = onClick,
            modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            shape = shape,
            color = color,
            contentColor = contentColor,
            interactionSource = interaction
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, contentColor = contentColor) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun AtLabel(text: String, modifier: Modifier = Modifier, color: Color = AtColors.Muted) {
    Text(text.uppercase(PT_BR), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
fun AtCardTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtColors.Muted)
            }
        }
        trailing()
    }
}

// ─── Botões ──────────────────────────────────────────────────────────────────

enum class AtButtonStyle { Dark, Light, Accent, Soft, Danger }

/** Botão em pílula (ex.: "Send" preto do layout). */
@Composable
fun AtPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AtButtonStyle = AtButtonStyle.Dark,
    icon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false
) {
    val bg = when (style) {
        AtButtonStyle.Dark -> AtColors.Primary
        AtButtonStyle.Light -> AtColors.Card
        AtButtonStyle.Accent -> AtColors.AccentSoft
        AtButtonStyle.Soft -> AtColors.Field
        AtButtonStyle.Danger -> Color(0xFFC62828)
    }
    val fg = when (style) {
        AtButtonStyle.Dark, AtButtonStyle.Danger -> Color.White
        AtButtonStyle.Accent -> AtColors.Primary
        else -> AtColors.Ink
    }
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val active = enabled && !loading
    Surface(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .heightIn(min = if (compact) 38.dp else 52.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            },
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = if (style == AtButtonStyle.Light) BorderStroke(1.dp, AtColors.Line) else null,
        interactionSource = interaction
    ) {
        Row(
            Modifier.padding(
                horizontal = if (compact) 16.dp else 22.dp,
                vertical = if (compact) 8.dp else 14.dp
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
            } else {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (icon != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(icon, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Botão redondo (ícones do topo: tema, notificações, voltar...). */
@Composable
fun AtCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    size: Dp = 44.dp,
    dot: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Box(modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = if (dark) AtColors.Primary else AtColors.Card,
            contentColor = if (dark) Color.White else AtColors.Ink,
            border = if (dark) null else BorderStroke(1.dp, AtColors.Line),
            interactionSource = interaction
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, Modifier.size(size * 0.45f))
            }
        }
        if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = size * 0.18f, end = size * 0.2f)
                    .size(8.dp)
                    .background(AtColors.Accent, CircleShape)
            )
        }
    }
}

/** Cabeçalho das telas internas: voltar + título + ações. */
@Composable
fun AtHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            AtCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", onBack)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AtColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

// ─── Campos ──────────────────────────────────────────────────────────────────

@Composable
fun atFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AtColors.Field,
    unfocusedContainerColor = AtColors.Field,
    disabledContainerColor = AtColors.Field,
    errorContainerColor = AtColors.DangerSoft,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = AtColors.Muted,
    unfocusedLabelColor = AtColors.Muted,
    focusedTextColor = AtColors.Ink,
    unfocusedTextColor = AtColors.Ink,
    cursorColor = AtColors.Ink,
    focusedLeadingIconColor = AtColors.Ink,
    unfocusedLeadingIconColor = AtColors.Muted,
    focusedTrailingIconColor = AtColors.Ink,
    unfocusedTrailingIconColor = AtColors.Muted,
    focusedPlaceholderColor = AtColors.Faint,
    unfocusedPlaceholderColor = AtColors.Faint
)

/** Campo preenchido cinza, sem borda, cantos arredondados. */
@Composable
fun AtField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, null, Modifier.size(20.dp)) }
        } else null,
        trailingIcon = trailing,
        singleLine = singleLine,
        readOnly = readOnly,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(18.dp),
        colors = atFieldColors()
    )
}

// ─── Blocos ──────────────────────────────────────────────────────────────────

/** Bloco quadrado de atalho (Lock / Voice / Bluetooth... do layout). */
@Composable
fun AtTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(22.dp),
        // Critério único: todo atalho é um cartão branco com ícone azul.
        // O azul cheio fica reservado para a ação principal da tela (ex.: "Abastecer" na Home).
        color = AtColors.Card,
        contentColor = AtColors.Ink,
        interactionSource = interaction
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(AtColors.AccentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(24.dp), tint = AtColors.Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class TileSpec(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun AtTileGrid(tiles: List<TileSpec>, columns: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    AtTile(t.label, t.icon, t.onClick, Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Seletor em pílula com item ativo preto (R · P · N · D · S do layout). */
@Composable
fun AtSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AtColors.Card
) {
    Surface(modifier.fillMaxWidth(), shape = CircleShape, color = container) {
        Row(Modifier.padding(5.dp)) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val bg by animateColorAsState(if (selected) AtColors.Primary else Color.Transparent, tween(260), label = "segBg")
                val fg by animateColorAsState(if (selected) Color.White else AtColors.Ink, tween(260), label = "segFg")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Pílula selecionável (filtros e escolha de item). */
@Composable
fun AtChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val bg by animateColorAsState(if (selected) AtColors.Primary else AtColors.Field, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else AtColors.Ink, tween(220), label = "chipFg")
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Linha de lista com ícone em caixinha (itens do "AI Assistant" do layout). */
@Composable
fun AtListRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = AtColors.Field,
    iconBg: Color = AtColors.Card,
    iconTint: Color = AtColors.Ink,
    subtitleLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtColors.Muted,
                    maxLines = subtitleLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

/** Faixa de aviso (sucesso em verde, erro em vermelho). */
@Composable
fun AtMessage(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isError) AtColors.DangerSoft else AtColors.AccentSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
            null,
            Modifier.size(18.dp),
            tint = if (isError) AtColors.Danger else AtColors.Primary
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = AtColors.Ink)
    }
}

/** Mini estatística: rótulo pequeno em caixa alta + valor. */
@Composable
fun AtStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        AtLabel(label)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Gráficos ────────────────────────────────────────────────────────────────

/** Medidor em arco (como o "Fuel Level 65%" do layout). [fraction] de 0 a 1. */
@Composable
fun AtGauge(
    fraction: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    progressColor: Color = AtColors.Primary
) {
    val anim by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        tween(1000, easing = FastOutSlowInEasing),
        label = "gauge"
    )
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        ) {
            val sw = strokeWidth.toPx()
            val d = size.width - sw
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(d, d)
            drawArc(
                color = AtColors.Line,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(sw)
            )
            if (anim > 0f) {
                val light = if (progressColor == AtColors.Primary) AtColors.Accent else progressColor.copy(alpha = 0.55f)
                drawArc(
                    brush = Brush.horizontalGradient(listOf(progressColor, light), 0f, size.width),
                    startAngle = 180f,
                    sweepAngle = 180f * anim,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(sw)
                )
            }
            val angle = Math.toRadians(180.0 + 180.0 * anim)
            val c = Offset(size.width / 2f, sw / 2f + d / 2f)
            val rIn = d / 2f - sw / 2f - 3.dp.toPx()
            val rOut = d / 2f + sw / 2f + 3.dp.toPx()
            drawLine(
                color = AtColors.InkSoft,
                start = c + Offset((cos(angle) * rIn).toFloat(), (sin(angle) * rIn).toFloat()),
                end = c + Offset((cos(angle) * rOut).toFloat(), (sin(angle) * rOut).toFloat()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(centerText, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
    }
}

/** Trilho com marcações e um botão redondo (o controle "10°C — 40°C" do layout). */
@Composable
fun AtTrack(fraction: Float, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "track")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        val y = size.height / 2f
        val ticks = 24
        for (i in 0..ticks) {
            val x = size.width * i / ticks
            val h = if (i % 6 == 0) 7.dp.toPx() else 4.dp.toPx()
            drawLine(AtColors.Faint, Offset(x, y - h), Offset(x, y + h), strokeWidth = 1.dp.toPx())
        }
        val r = 9.dp.toPx()
        val kx = (size.width * anim).coerceIn(r, size.width - r)
        drawCircle(Color.White, r, Offset(kx, y))
        drawCircle(AtColors.Ink, r, Offset(kx, y), style = Stroke(1.5.dp.toPx()))
        drawCircle(AtColors.Ink, 3.dp.toPx(), Offset(kx, y))
    }
}

/** Barras estilo "onda de áudio" (player do layout). Parte inicial colorida conforme [fraction]. */
@Composable
fun AtWaveform(fraction: Float, modifier: Modifier = Modifier, bars: Int = 42, seed: Int = 11) {
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "wave")
    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val rnd = Random(seed)
        val step = size.width / bars
        val bw = step * 0.55f
        for (i in 0 until bars) {
            val h = size.height * (0.25f + rnd.nextFloat() * 0.75f)
            val x = i * step + step / 2f
            val colored = (i + 0.5f) / bars <= anim
            drawLine(
                color = if (colored) AtColors.Primary else AtColors.Line,
                start = Offset(x, (size.height - h) / 2f),
                end = Offset(x, (size.height + h) / 2f),
                strokeWidth = bw,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Mapa decorativo (ruas + rota + marcador verde) usado no cartão de serviços. */
@Composable
fun AtDecorMap(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFFF1F1F1))
            val rnd = Random(7)
            val w = size.width
            val h = size.height
            repeat(16) {
                val p = Path()
                val y0 = rnd.nextFloat() * h
                p.moveTo(-10f, y0)
                var x = 0f
                var y = y0
                while (x < w) {
                    x += w * (0.15f + rnd.nextFloat() * 0.2f)
                    y += (rnd.nextFloat() - 0.5f) * h * 0.35f
                    p.lineTo(x, y)
                }
                drawPath(p, Color(0xFFDDDDDD), style = Stroke(1.dp.toPx()))
            }
            repeat(10) {
                val p = Path()
                val x0 = rnd.nextFloat() * w
                p.moveTo(x0, -10f)
                var x = x0
                var y = 0f
                while (y < h) {
                    y += h * (0.2f + rnd.nextFloat() * 0.25f)
                    x += (rnd.nextFloat() - 0.5f) * w * 0.25f
                    p.lineTo(x, y)
                }
                drawPath(p, Color(0xFFDDDDDD), style = Stroke(1.dp.toPx()))
            }
            repeat(3) { i ->
                val p = Path()
                p.moveTo(-10f, h * (0.25f + 0.3f * i))
                p.cubicTo(w * 0.3f, h * (0.1f + 0.35f * i), w * 0.6f, h * (0.5f + 0.2f * i), w + 10f, h * (0.3f + 0.25f * i))
                drawPath(p, Color.White, style = Stroke(6.dp.toPx()))
            }
            val route = Path().apply {
                moveTo(w * 0.2f, h * 0.42f)
                lineTo(w * 0.44f, h * 0.3f)
                lineTo(w * 0.6f, h * 0.36f)
                lineTo(w * 0.72f, h * 0.5f)
            }
            drawPath(
                route, AtColors.InkSoft,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(AtColors.Ink, 5.dp.toPx(), Offset(w * 0.72f, h * 0.5f))
        }
        Box(
            Modifier
                .offset(x = maxWidth * 0.2f - 20.dp, y = maxHeight * 0.42f - 20.dp)
                .size(40.dp)
                .background(AtColors.Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Navigation, null,
                Modifier
                    .size(20.dp)
                    .rotate(60f),
                tint = AtColors.Ink
            )
        }
    }
}

// ─── Identidade ──────────────────────────────────────────────────────────────

/** Logo do app (assets/branding/logo.png) com reserva em círculo preto. */
@Composable
fun AtLogo(size: Dp = 40.dp) {
    val context = LocalContext.current
    val bitmap = remember {
        runCatching {
            context.assets.open("branding/logo.png").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), "Logo AutoTech", Modifier.size(size))
    } else {
        Box(Modifier.size(size).background(AtColors.Ink, CircleShape), contentAlignment = Alignment.Center) {
            Text("A", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/** Avatar redondo com as iniciais do usuário. */
@Composable
fun AtAvatar(name: String, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .background(AtColors.Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initialsOf(name), color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** Placa no padrão Mercosul. */
@Composable
fun AtPlate(plate: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val text = plate.ifBlank { "ABC1D23" }
    Surface(
        modifier.width(if (compact) 104.dp else 216.dp),
        shape = RoundedCornerShape(if (compact) 6.dp else 12.dp),
        color = Color.White,
        border = BorderStroke(if (compact) 1.5.dp else 2.5.dp, AtColors.Ink)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (compact) 10.dp else 20.dp)
                    .background(AtColors.PlateBlue),
                contentAlignment = Alignment.Center
            ) {
                val fs = if (compact) 6.sp else 10.sp
                Text(
                    "BRASIL",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = fs,
                        lineHeight = fs,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }
            val ps = if (compact) 18.sp else 36.sp
            Text(
                text.uppercase(PT_BR),
                style = TextStyle(
                    color = if (plate.isBlank()) AtColors.Faint else AtColors.Ink,
                    fontSize = ps,
                    lineHeight = ps,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = if (compact) 1.sp else 3.sp
                ),
                modifier = Modifier.padding(vertical = if (compact) 3.dp else 8.dp)
            )
        }
    }
}

/** "Palco" com o carro em degradê e sombra no chão, no lugar da foto do layout. */
@Composable
fun AtCarStage(modifier: Modifier = Modifier, iconSize: Dp = 150.dp, icon: ImageVector) {
    Box(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            Modifier
                .fillMaxWidth(0.8f)
                .height(iconSize * 0.9f)
        ) {
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0x33000000), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.86f),
                    radius = size.width / 2f
                ),
                topLeft = Offset(0f, size.height * 0.74f),
                size = Size(size.width, size.height * 0.24f)
            )
        }
        Icon(
            icon, null,
            Modifier
                .size(iconSize)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(listOf(Color(0xFF6B6B6B), Color(0xFF151515))),
                        blendMode = BlendMode.SrcAtop
                    )
                },
            tint = AtColors.Ink
        )
    }
}
