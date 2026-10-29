package com.autotech.android.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Place
import com.autotech.android.ui.components.AtButtonStyle
import com.autotech.android.ui.components.AtCard
import com.autotech.android.ui.components.AtChip
import com.autotech.android.ui.components.AtCircleButton
import com.autotech.android.ui.components.AtListRow
import com.autotech.android.ui.components.AtMessage
import com.autotech.android.ui.components.AtPillButton
import com.autotech.android.ui.components.AtScreen
import com.autotech.android.ui.components.AtSegmented
import com.autotech.android.ui.theme.AtColors

// ─── Modelos locais ───────────────────────────────────────────────────────────

internal enum class PlaceFilter(
    val label: String,
    val query: String,
    val icon: ImageVector
) {
    OFICINAS("Oficinas", "oficina mecânica", Icons.Default.Build),
    BORRACHARIAS("Borracharias", "borracharia", Icons.Default.Construction),
    POSTOS("Postos", "posto de combustível", Icons.Default.LocalGasStation)
}

/** Estabelecimento retornado pela busca, já com a distância calculada a partir do usuário. */
internal data class NearbyPlace(
    val name: String,
    val position: LatLng,
    val address: String?,
    val rating: Double?,
    val ratingCount: Int?,
    val distanceMeters: Float
)

/** RF07: opções de raio de busca, em km. */
private val RADIUS_OPTIONS_KM = listOf(2, 5, 10, 20)

/** RNF03: tempo máximo de espera da busca no Google Places. */
internal const val SEARCH_TIMEOUT_MS = 5_000L

private val PT_BR = Locale("pt", "BR")

internal fun formatDistance(meters: Float): String =
    if (meters < 1000f) "${meters.toInt()} m"
    else String.format(PT_BR, "%.1f km", meters / 1000f)

internal fun formatRating(rating: Double?, count: Int?): String? {
    if (rating == null) return null
    val r = String.format(PT_BR, "%.1f", rating)
    return if (count != null) "★ $r ($count avaliações)" else "★ $r"
}

// ─── Tela ─────────────────────────────────────────────────────────────────────

/**
 * RF07 — Localização de serviços: busca de oficinas, borracharias e postos dentro de um
 * raio configurável a partir da posição atual, com lista ordenada por distância
 * (nome, distância, avaliação e endereço) e redirecionamento ao app de navegação.
 *
 * Requer MAPS_API_KEY no local.properties, com "Maps SDK for Android" e
 * "Places API (New)" habilitados no Google Cloud.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // ── Estado de localização ─────────────────────────────────────────────────
    val defaultPos = remember { LatLng(-23.5505, -46.6333) }   // São Paulo (antes do GPS responder)
    var userPos by remember { mutableStateOf<LatLng?>(null) }

    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    userPos = LatLng(loc.latitude, loc.longitude)
                }
            }
        }
    }

    // ── Permissões ────────────────────────────────────────────────────────────
    val permissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }

    DisposableEffect(permissions.allPermissionsGranted) {
        if (permissions.allPermissionsGranted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .build()
            locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            locationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { userPos = LatLng(it.latitude, it.longitude) }
            }
        }
        onDispose { locationClient.removeLocationUpdates(locationCallback) }
    }

    // ── Câmera: centraliza no usuário só na primeira localização ─────────────
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 13f)
    }
    var cameraCentered by remember { mutableStateOf(false) }
    LaunchedEffect(userPos) {
        val pos = userPos ?: return@LaunchedEffect
        if (!cameraCentered) {
            cameraCentered = true
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        }
    }

    // ── Places SDK ────────────────────────────────────────────────────────────
    val placesClient = remember {
        if (!Places.isInitialized()) {
            val apiKey = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
            Places.initializeWithNewPlacesApiEnabled(context, apiKey)
        }
        Places.createClient(context)
    }

    // ── Estado da busca ───────────────────────────────────────────────────────
    var activeFilter by remember { mutableStateOf<PlaceFilter?>(null) }
    var radiusKm by remember { mutableStateOf(5) }
    var results by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    // Identifica a busca mais recente: respostas atrasadas ou de buscas anteriores são ignoradas.
    var currentRequestId by remember { mutableStateOf(0) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { mainHandler.removeCallbacksAndMessages(null) } }

    fun searchNearby(filter: PlaceFilter, radius: Int) {
        val origin = userPos
        if (origin == null) {
            searchError = "Aguardando sua localização. Tente novamente em instantes."
            return
        }
        val requestId = currentRequestId + 1
        currentRequestId = requestId
        searchLoading = true
        searchError = null
        results = emptyList()

        val radiusMeters = radius * 1000.0
        val request = SearchByTextRequest.builder(
            filter.query,
            listOf(
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.RATING,
                Place.Field.USER_RATING_COUNT
            )
        )
            .setMaxResultCount(20)
            .setLocationBias(CircularBounds.newInstance(origin, radiusMeters))
            .build()

        // RNF03: encerra a espera após 5 segundos sem resposta.
        mainHandler.postDelayed({
            if (currentRequestId == requestId && searchLoading) {
                currentRequestId = requestId + 1
                searchLoading = false
                searchError = "A busca passou de 5 segundos. Verifique a conexão e tente novamente."
            }
        }, SEARCH_TIMEOUT_MS)

        placesClient.searchByText(request)
            .addOnSuccessListener { response ->
                if (currentRequestId != requestId) return@addOnSuccessListener
                val found = response.places.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    val dist = FloatArray(1)
                    Location.distanceBetween(
                        origin.latitude, origin.longitude, loc.latitude, loc.longitude, dist
                    )
                    NearbyPlace(
                        name = place.displayName ?: filter.label,
                        position = LatLng(loc.latitude, loc.longitude),
                        address = place.formattedAddress,
                        rating = place.rating,
                        ratingCount = place.userRatingCount,
                        distanceMeters = dist[0]
                    )
                }
                    .filter { it.distanceMeters <= radiusMeters }
                    .sortedBy { it.distanceMeters }
                results = found
                if (found.isEmpty()) searchError = "Nenhum resultado em até $radius km."
                searchLoading = false
            }
            .addOnFailureListener { e ->
                if (currentRequestId != requestId) return@addOnFailureListener
                searchError = "Erro ao buscar locais: ${e.localizedMessage}"
                searchLoading = false
            }
    }

    /** RF07: abre a rota no aplicativo de navegação padrão do dispositivo. */
    fun openNavigation(place: NearbyPlace) {
        val lat = place.position.latitude
        val lng = place.position.longitude
        val geo = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(place.name)})")
        )
        try {
            context.startActivity(geo)
        } catch (_: ActivityNotFoundException) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            )
            runCatching { context.startActivity(web) }
                .onFailure { searchError = "Nenhum aplicativo de navegação encontrado." }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    // Permissão negada com possibilidade de pedir novamente
    if (!permissions.allPermissionsGranted && permissions.shouldShowRationale) {
        AtScreen(verticalArrangement = Arrangement.Center) {
            AtCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(AtColors.Accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Place, null, tint = AtColors.Ink)
                }
                Spacer(Modifier.height(16.dp))
                Text("Precisamos da sua localização", style = MaterialTheme.typography.titleLarge)
                Text(
                    "O acesso à localização é necessário para mostrar serviços próximos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtColors.Muted
                )
                Spacer(Modifier.height(18.dp))
                AtPillButton(
                    "Conceder permissão",
                    { permissions.launchMultiplePermissionRequest() },
                    Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                AtPillButton("Voltar", onBack, Modifier.fillMaxWidth(), style = AtButtonStyle.Light)
            }
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AtColors.Background)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(isMyLocationEnabled = permissions.allPermissionsGranted),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = permissions.allPermissionsGranted
            ),
            contentPadding = PaddingValues(top = 120.dp, bottom = 330.dp),
            cameraPositionState = cameraPositionState
        ) {
            results.forEach { place ->
                Marker(
                    state = MarkerState(position = place.position),
                    title = place.name,
                    snippet = "${formatDistance(place.distanceMeters)} — toque para traçar a rota",
                    onInfoWindowClick = { openNavigation(place) }
                )
            }
        }

        // ── Topo: voltar + filtros por tipo ──────────────────────────────────
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AtCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", onBack, size = 48.dp)
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = AtColors.Card,
                shadowElevation = 6.dp
            ) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlaceFilter.entries.forEach { filter ->
                        AtChip(
                            label = filter.label,
                            selected = activeFilter == filter,
                            icon = filter.icon,
                            onClick = {
                                if (activeFilter == filter) {
                                    activeFilter = null
                                    currentRequestId += 1
                                    searchLoading = false
                                    results = emptyList()
                                    searchError = null
                                } else {
                                    activeFilter = filter
                                    searchNearby(filter, radiusKm)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (searchLoading) {
            Surface(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
                shape = CircleShape,
                color = AtColors.Card,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = AtColors.Ink, strokeWidth = 3.dp)
                }
            }
        }

        // ── Painel inferior: raio + lista ordenada por distância ─────────────
        Surface(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = AtColors.Card,
            shadowElevation = 12.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(AtColors.Faint, CircleShape)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Serviços próximos", style = MaterialTheme.typography.titleLarge)
                        Text(
                            activeFilter?.let { "${it.label} em até $radiusKm km" } ?: "Escolha um tipo de serviço no topo",
                            style = MaterialTheme.typography.bodySmall,
                            color = AtColors.Muted
                        )
                    }
                    if (results.isNotEmpty()) {
                        Box(
                            Modifier
                                .background(AtColors.Accent, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("${results.size}", style = MaterialTheme.typography.labelLarge, color = AtColors.Ink)
                        }
                    }
                }

                AtSegmented(
                    options = RADIUS_OPTIONS_KM.map { "$it km" },
                    selectedIndex = RADIUS_OPTIONS_KM.indexOf(radiusKm).coerceAtLeast(0),
                    onSelect = { i ->
                        val km = RADIUS_OPTIONS_KM[i]
                        radiusKm = km
                        activeFilter?.let { searchNearby(it, km) }
                    },
                    container = AtColors.Field
                )

                if (!permissions.allPermissionsGranted && !permissions.shouldShowRationale) {
                    AtMessage(
                        "Localização negada. Ative nas Configurações do sistema para buscar serviços próximos.",
                        isError = true
                    )
                }

                searchError?.let { AtMessage(it, isError = true) }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { "${it.name}-${it.position.latitude}-${it.position.longitude}" }) { place ->
                        AtListRow(
                            title = place.name,
                            subtitle = listOfNotNull(
                                listOfNotNull(
                                    formatDistance(place.distanceMeters),
                                    formatRating(place.rating, place.ratingCount)
                                ).joinToString(" · "),
                                place.address
                            ).joinToString("\n"),
                            subtitleLines = 2,
                            icon = activeFilter?.icon ?: Icons.Rounded.Place,
                            iconBg = AtColors.AccentSoft,
                            onClick = { openNavigation(place) },
                            trailing = {
                                AtCircleButton(
                                    Icons.Rounded.Navigation,
                                    "Traçar rota",
                                    { openNavigation(place) },
                                    dark = true,
                                    size = 38.dp
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
