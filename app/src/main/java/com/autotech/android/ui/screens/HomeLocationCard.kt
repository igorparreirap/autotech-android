package com.autotech.android.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtCardTitle
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.theme.AtColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * RF-07 na tela inicial: mapa pequeno com a posição atual do usuário
 * (Maps SDK for Android, sem custo por carregamento). Este cartão NUNCA consulta o
 * Google Places: a busca de oficinas, borracharias e postos só acontece quando o
 * usuário abre a tela de Localização.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeLocationCard(onOpenFullMap: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val granted = permissions.permissions.any { it.status.isGranted }
    var userPos by remember { mutableStateOf<LatLng?>(null) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val callback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { userPos = LatLng(it.latitude, it.longitude) }
            }
        }
    }
    DisposableEffect(granted) {
        if (granted) {
            locationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { userPos = LatLng(it.latitude, it.longitude) }
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000L)
                .setMinUpdateIntervalMillis(10_000L)
                .build()
            locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
        onDispose { locationClient.removeLocationUpdates(callback) }
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-15.78, -47.93), 3.5f) // Brasil, até o GPS responder
    }
    LaunchedEffect(userPos) {
        userPos?.let { cameraState.position = CameraPosition.fromLatLngZoom(it, 15f) }
    }

    AtCard(modifier.fillMaxWidth()) {
        AtCardTitle("Localização", "Sua posição atual") {
            AtCircleButton(Icons.Rounded.Search, "Buscar serviços próximos", onOpenFullMap, size = 36.dp)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AtColors.Field)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = granted),
                uiSettings = MapUiSettings(
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    // gestos desligados para não brigar com a rolagem da tela inicial
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = false
                ),
                onMapClick = { onOpenFullMap() }
            )

            if (!granted) {
                Pill(
                    "Mostrar minha localização", Icons.Rounded.MyLocation,
                    { permissions.launchMultiplePermissionRequest() },
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    dark = false
                )
            }
            Pill(
                "Buscar serviços", Icons.Rounded.Search, onOpenFullMap,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                dark = true
            )
        }
    }
}

@Composable
private fun Pill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    dark: Boolean
) {
    val fg = if (dark) Color.White else AtColors.Ink
    Row(
        modifier
            .clip(CircleShape)
            .background(if (dark) AtColors.Primary else AtColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
