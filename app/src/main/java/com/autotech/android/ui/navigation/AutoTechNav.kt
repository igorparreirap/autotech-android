package com.autotech.android.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.components.AtLogo
import com.autotech.android.ui.theme.AtColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autotech.android.AutoTechApplication
import com.autotech.android.ui.screens.AddFuelScreen
import com.autotech.android.ui.screens.ForgotPasswordScreen
import com.autotech.android.ui.screens.PdfViewerScreen
import com.autotech.android.ui.screens.AddMaintenanceScreen
import com.autotech.android.ui.screens.AddVehicleScreen
import com.autotech.android.ui.screens.AuthScreen
import com.autotech.android.ui.screens.DashboardScreen
import com.autotech.android.ui.screens.FuelListScreen
import com.autotech.android.ui.screens.HomeScreen
import com.autotech.android.ui.screens.MaintenanceListScreen
import com.autotech.android.ui.screens.MapScreen
import com.autotech.android.ui.screens.NotificationsScreen
import com.autotech.android.ui.screens.SearchScreen
import com.autotech.android.ui.screens.VehicleDetailScreen
import com.autotech.android.ui.screens.VehicleListScreen
import androidx.compose.runtime.rememberCoroutineScope // Add this
import kotlinx.coroutines.launch // Add this

@Composable
fun AutoTechRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as AutoTechApplication
    val nav = rememberNavController()
    val scope = rememberCoroutineScope() // 1. Create the scope here
    // rememberSaveable: o nome sobrevive à recriação da Activity (rotação da tela, troca de tema).
    var displayName by rememberSaveable { mutableStateOf("usuário") }

    // RF-06: toque numa notificação -> histórico de manutenções do veículo (depois do login/splash)
    val pendingVehicle by PendingOpen.maintenanceVehicleId
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(pendingVehicle, currentRoute) {
        val id = pendingVehicle ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == NavRoutes.SPLASH || currentRoute == NavRoutes.AUTH) return@LaunchedEffect
        if (app.currentUserId == null) return@LaunchedEffect
        PendingOpen.maintenanceVehicleId.value = null
        nav.navigate(NavRoutes.maintenanceList(id)) { launchSingleTop = true }
    }

    NavHost(
        navController = nav,
        startDestination = NavRoutes.SPLASH,
        modifier = Modifier.background(AtColors.Background),
        enterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 6 }
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 6 }
        },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 6 }
        }
    ) {
        composable(NavRoutes.SPLASH) {
            LaunchedEffect(Unit) {
                app.restoreSession()
                // A sessão salva pode apontar para um usuário que não existe mais no banco
                // (ex.: banco recriado após mudança de versão). Nesse caso, volta para o login.
                val user = app.currentUserId?.let { app.graph.authRepository.getUser(it) }
                if (user == null) app.setSession(null)
                if (user != null) {
                    displayName = user.displayName
                    nav.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                } else {
                    nav.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AtColors.Background),
                contentAlignment = Alignment.Center
            ) {
                AtLogo(72.dp)
            }
        }

        composable(
            NavRoutes.FORGOT_PASSWORD,
            listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            ForgotPasswordScreen(
                initialEmail = entry.arguments?.getString("email").orEmpty(),
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
        composable(NavRoutes.AUTH) {
            AuthScreen(
                onForgotPassword = { email -> nav.navigate(NavRoutes.forgotPassword(email)) },
                onSignedIn = {
                    val uid = app.currentUserId!!

                    // 2. Wrap the suspend call in a launch block
                    scope.launch {
                        displayName = app.graph.authRepository.getUser(uid)?.displayName ?: "usuário"

                        // Move navigation inside the launch block to ensure
                        // the name is fetched before moving screens
                        nav.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.AUTH) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(NavRoutes.HOME) {
            val uid = app.currentUserId
            if (uid == null) {
                LaunchedEffect(Unit) {
                    nav.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
                return@composable
            }
            // O nome exibido vem sempre do banco local para o usuário da sessão,
            // e não só do login ou do splash, que não rodam de novo quando a tela é recriada.
            val userName by produceState(initialValue = displayName, uid) {
                app.graph.authRepository.getUser(uid)?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { value = it; displayName = it }
            }
            HomeScreen(
                userId = uid,
                userName = userName,
                onVehicles = { nav.navigate(NavRoutes.VEHICLES) },
                onAddVehicle = { nav.navigate(NavRoutes.VEHICLE_ADD) },
                onOpenVehicle = { id -> nav.navigate(NavRoutes.vehicleDetail(id)) },
                onMaintenance = { id -> nav.navigate(NavRoutes.maintenanceList(id)) },
                onFuel = { id -> nav.navigate(NavRoutes.fuelList(id)) },
                onFuelAdd = { id -> nav.navigate(NavRoutes.fuelAdd(id)) },
                onDashboard = { nav.navigate(NavRoutes.DASHBOARD) },
                onMap = { nav.navigate(NavRoutes.MAP) },
                onNotifications = { nav.navigate(NavRoutes.NOTIFICATIONS) },
                onSearch = { nav.navigate(NavRoutes.SEARCH) },
                onOpenPdf = { path -> nav.navigate(NavRoutes.pdfView(path)) },
                onCompletePending = { vehicleId, pendingId -> nav.navigate(NavRoutes.maintenanceAdd(vehicleId, pendingId)) },
                onSignOut = {
                    app.setSession(null)
                    nav.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.VEHICLES) {
            val uid = app.currentUserId ?: return@composable
            VehicleListScreen(
                userId = uid,
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(NavRoutes.VEHICLE_ADD) },
                onOpen = { id -> nav.navigate(NavRoutes.vehicleDetail(id)) }
            )
        }
        composable(NavRoutes.VEHICLE_ADD) {
            val uid = app.currentUserId ?: return@composable
            AddVehicleScreen(
                userId = uid,
                onDone = { nav.popBackStack() },
                onCancel = { nav.popBackStack() }
            )
        }
        composable(
            NavRoutes.VEHICLE_DETAIL,
            listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("pdf") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val id = entry.arguments!!.getLong("vehicleId")
            VehicleDetailScreen(
                vehicleId = id,
                openPdfOnStart = entry.arguments?.getBoolean("pdf") ?: false,
                onBack = { nav.popBackStack() },
                onMaintenance = { nav.navigate(NavRoutes.maintenanceList(id)) },
                onFuel = { nav.navigate(NavRoutes.fuelList(id)) },
                // depois de excluir, volta para a tela inicial
                onDeleted = { nav.popBackStack(NavRoutes.HOME, false) },
                onOpenPdf = { path -> nav.navigate(NavRoutes.pdfView(path)) }
            )
        }
        composable(
            NavRoutes.PDF_VIEW,
            listOf(navArgument("path") { type = NavType.StringType })
        ) { entry ->
            PdfViewerScreen(
                path = entry.arguments?.getString("path").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            NavRoutes.MAINTENANCE_LIST,
            listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments!!.getLong("vehicleId")
            MaintenanceListScreen(
                vehicleId = id,
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(NavRoutes.maintenanceAdd(id)) },
                onCompletePending = { pendingId -> nav.navigate(NavRoutes.maintenanceAdd(id, pendingId)) }
            )
        }
        composable(
            NavRoutes.MAINTENANCE_ADD,
            listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("pending") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { entry ->
            val id = entry.arguments!!.getLong("vehicleId")
            AddMaintenanceScreen(
                vehicleId = id,
                pendingId = entry.arguments?.getLong("pending") ?: -1L,
                onDone = { nav.popBackStack() },
                onCancel = { nav.popBackStack() }
            )
        }
        composable(
            NavRoutes.FUEL_LIST,
            listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments!!.getLong("vehicleId")
            FuelListScreen(
                vehicleId = id,
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(NavRoutes.fuelAdd(id)) }
            )
        }
        composable(
            NavRoutes.FUEL_ADD,
            listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments!!.getLong("vehicleId")
            AddFuelScreen(
                vehicleId = id,
                onDone = { nav.popBackStack() },
                onCancel = { nav.popBackStack() }
            )
        }
        composable(NavRoutes.DASHBOARD) {
            val uid = app.currentUserId ?: return@composable
            DashboardScreen(userId = uid, onBack = { nav.popBackStack() })
        }
        composable(NavRoutes.SEARCH) {
            val uid = app.currentUserId ?: return@composable
            SearchScreen(
                userId = uid,
                onBack = { nav.popBackStack() },
                onNavigate = { route ->
                    nav.navigate(route) { popUpTo(NavRoutes.SEARCH) { inclusive = true } }
                }
            )
        }
        composable(NavRoutes.NOTIFICATIONS) {
            val uid = app.currentUserId ?: return@composable
            NotificationsScreen(
                userId = uid,
                onBack = { nav.popBackStack() },
                onOpenMaintenance = { id -> nav.navigate(NavRoutes.maintenanceList(id)) },
                onCompletePending = { vehicleId, pendingId -> nav.navigate(NavRoutes.maintenanceAdd(vehicleId, pendingId)) }
            )
        }
        composable(NavRoutes.MAP) {
            MapScreen(onBack = { nav.popBackStack() })
        }
    }
}
