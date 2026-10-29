package com.autotech.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtHeader
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.PdfPages
import com.autotech.android.util.VehicleReportPdf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RF-10: visualizador do relatório dentro do aplicativo, com as opções de
 * compartilhar (WhatsApp, e-mail...) e de abrir em outro leitor de PDF.
 */
@Composable
fun PdfViewerScreen(path: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    val pages = remember(path) { PdfPages.open(file) }
    DisposableEffect(pages) { onDispose { pages?.close() } }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val subtitle = remember(path) {
        "Gerado em " + SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(file.lastModified())) +
            (pages?.let { " · ${it.count} ${if (it.count == 1) "página" else "páginas"}" } ?: "")
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AtColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AtHeader("Relatório em PDF", subtitle, onBack = onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtPillButton(
                "Compartilhar",
                { VehicleReportPdf.share(context, file) },
                Modifier.weight(1f),
                leadingIcon = Icons.Rounded.Share,
                enabled = file.exists()
            )
            AtPillButton(
                "Abrir com...",
                { VehicleReportPdf.openWith(context, file) },
                Modifier.weight(1f),
                style = AtButtonStyle.Light,
                leadingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                enabled = file.exists()
            )
        }

        if (pages == null || pages.count == 0) {
            AtMessage("Não foi possível abrir este PDF. Ele pode ter sido apagado.", isError = true)
            return@Column
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                // Dois dedos: ampliar/reduzir. Com zoom, um dedo também arrasta para os lados.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val fingers = event.changes.count { it.pressed }
                            if (fingers >= 2) {
                                scale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                                event.changes.forEach { it.consume() }
                            }
                            if (scale > 1f) {
                                val limit = size.width * (scale - 1f) / 2f
                                offsetX = (offsetX + event.calculatePan().x).coerceIn(-limit, limit)
                            } else {
                                offsetX = 0f
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offsetX = 0f
                    })
                }
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }.coerceIn(400, 1600)
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pages.count) { index -> PdfPageItem(pages, index, widthPx) }
            }
        }
        Text(
            "Toque duas vezes ou use dois dedos para ampliar",
            style = MaterialTheme.typography.labelSmall,
            color = AtColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PdfPageItem(pages: PdfPages, index: Int, widthPx: Int) {
    var bitmap by remember(index, widthPx) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(index, widthPx) {
        bitmap = pages.render(index, widthPx)?.asImageBitmap()
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / pages.ratio(index))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
    ) {
        val b = bitmap
        if (b != null) {
            Image(b, "Página ${index + 1}", Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(
                Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
                color = AtColors.Primary,
                strokeWidth = 2.dp
            )
        }
    }
}
