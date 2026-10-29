package com.autotech.android.ui.screens

import com.autotech.android.domain.FuelComparison
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtField
import com.autotech.android.ui.components.AtLabel
import com.autotech.android.ui.components.PT_BR
import com.autotech.android.ui.components.formatMoney
import com.autotech.android.ui.components.parseDecimal
import com.autotech.android.ui.theme.AtColors
import kotlin.math.abs


/** Mantém só dígitos e um separador decimal ("5,79" ou "5.79"). */
private fun sanitizePrice(raw: String): String {
    var sepUsed = false
    return buildString {
        for (c in raw) {
            if (c.isDigit()) append(c)
            else if ((c == ',' || c == '.') && !sepUsed) { append(c); sepUsed = true }
        }
    }.take(6)
}

/** Mini calculadora álcool x gasolina (regra dos 70%), com resultado na hora. */
@Composable
fun FuelCalculatorCard(modifier: Modifier = Modifier) {
    var gasoline by rememberSaveable { mutableStateOf("") }
    var ethanol by rememberSaveable { mutableStateOf("") }

    val g = parseDecimal(gasoline)?.takeIf { it > 0 }
    // RN-06: regra no domínio (domain/FuelComparison)
    val result = FuelComparison.compare(g, parseDecimal(ethanol))
    val ratio = result?.ratio
    val verdict = result?.verdict

    AtCard(modifier.fillMaxWidth()) {
        AtCardTitle("Álcool ou gasolina?", "Digite os preços do dia (R$ por litro)") {
            Icon(Icons.Rounded.Calculate, null, tint = AtColors.Ink)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtField(
                gasoline, { gasoline = sanitizePrice(it) }, "Gasolina",
                placeholder = "5,79",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            AtField(
                ethanol, { ethanol = sanitizePrice(it) }, "Álcool",
                placeholder = "3,99",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = verdict,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "fuelVerdict"
        ) { v ->
            if (v == null || ratio == null || g == null) {
                Text(
                    "O álcool compensa quando custa até 70% do preço da gasolina.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AtColors.Muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AtColors.Field)
                        .padding(14.dp)
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AtColors.Deep)
                        .padding(16.dp)
                ) {
                    AtLabel("Compensa abastecer com", color = Color(0xFF8E8E8E))
                    Text(
                        when (v) {
                            FuelComparison.Verdict.ETHANOL -> "Álcool"
                            FuelComparison.Verdict.GASOLINE -> "Gasolina"
                            FuelComparison.Verdict.EQUIVALENT -> "Tanto faz"
                        },
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (v == FuelComparison.Verdict.GASOLINE) Color.White else AtColors.Accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        String.format(PT_BR, "O álcool custa %.1f%% do preço da gasolina.", ratio * 100),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBDBDBD)
                    )
                    Spacer(Modifier.height(14.dp))
                    RatioBar(ratio.toFloat())
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Álcool vale a pena até ${formatMoney(g * FuelComparison.ETHANOL_LIMIT)} com a gasolina a ${formatMoney(g)}.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** Barra 0–100% com a marca dos 70%; verde se o álcool estiver abaixo do limite. */
@Composable
private fun RatioBar(ratio: Float) {
    val anim by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(500, easing = FastOutSlowInEasing), label = "ratio")
    val under = ratio <= FuelComparison.ETHANOL_LIMIT
    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333))
            )
            Box(
                Modifier
                    .fillMaxWidth(anim)
                    .height(6.dp)
                    .offset(y = 4.dp)
                    .clip(CircleShape)
                    .background(if (under) AtColors.Accent else Color.White)
            )
            Box(
                Modifier
                    .offset(x = maxWidth * FuelComparison.ETHANOL_LIMIT.toFloat() - 1.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .background(AtColors.Accent)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text("0%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E8E))
            Spacer(Modifier.weight(0.7f))
            Text("70%", style = MaterialTheme.typography.labelSmall, color = AtColors.Accent)
            Spacer(Modifier.weight(0.3f))
            Text("100%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E8E))
        }
    }
}
