package com.autotech.android.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.theme.AtColors
import com.autotech.android.util.VehiclePhotoStore
import kotlinx.coroutines.launch

/** Foto salva do veículo (ou null), recarregada sempre que alguma foto muda. */
@Composable
fun rememberVehiclePhoto(vehicleId: Long): ImageBitmap? {
    val context = LocalContext.current
    val version by VehiclePhotoStore.version.collectAsState()
    val photo by produceState<ImageBitmap?>(initialValue = null, vehicleId, version) {
        value = VehiclePhotoStore.load(context, vehicleId)
    }
    return photo
}

/**
 * Área do carro no cartão "Meu veículo": mostra a foto enviada pelo usuário ou,
 * sem foto, o carro desenhado padrão. O botão de câmera adiciona, troca ou remove a foto.
 */
@Composable
fun VehiclePhotoStage(
    vehicleId: Long,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photo = rememberVehiclePhoto(vehicleId)
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val ok = VehiclePhotoStore.save(context, vehicleId, uri)
                busy = false
                if (!ok) Toast.makeText(context, "Não foi possível usar essa foto.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Crossfade(targetState = photo, label = "vehiclePhoto") { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Foto do veículo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AtCarStage(iconSize = height * 0.88f, icon = Icons.Rounded.DirectionsCar)
                }
            }
        }

        if (busy) {
            CircularProgressIndicator(
                Modifier
                    .align(Alignment.Center)
                    .size(30.dp),
                color = AtColors.Ink,
                strokeWidth = 3.dp
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            AtCircleButton(
                icon = if (photo == null) Icons.Rounded.AddAPhoto else Icons.Rounded.PhotoCamera,
                contentDescription = if (photo == null) "Adicionar foto do carro" else "Trocar ou remover foto",
                onClick = {
                    if (photo == null) {
                        pick()
                    } else {
                        menu = true
                    }
                },
                dark = true,
                size = 40.dp
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Trocar foto") },
                    leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) },
                    onClick = { menu = false; pick() }
                )
                DropdownMenuItem(
                    text = { Text("Remover foto") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                    onClick = {
                        menu = false
                        scope.launch { VehiclePhotoStore.delete(context, vehicleId) }
                    }
                )
            }
        }
    }
}
