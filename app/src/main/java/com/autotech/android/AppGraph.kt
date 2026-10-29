package com.autotech.android

import com.autotech.android.data.remote.NetworkModule
import com.autotech.android.data.repository.AuthRepository
import com.autotech.android.data.repository.FuelRepository
import com.autotech.android.data.repository.MaintenanceRepository
import com.autotech.android.data.repository.PlateLookupRepository
import com.autotech.android.data.repository.VehicleRepository

/**
 * Grafo simples de dependências (Clean Architecture — camada de apresentação injeta via app).
 */
class AppGraph(application: AutoTechApplication) {

    private val db = application.database

    /** RF02: consulta de veículo pela placa (SimplesAPI) com cache local. */
    val plateLookupRepository = PlateLookupRepository(
        api = NetworkModule.simplesApi(logDebug = BuildConfig.DEBUG),
        cacheDao = db.plateLookupCacheDao(),
        apiKey = BuildConfig.SIMPLES_API_KEY
    )

    val vehicleRepository = VehicleRepository(
        vehicleDao = db.vehicleDao(),
        maintenanceDao = db.maintenanceDao(),
        fuelDao = db.fuelDao()
    )

    val authRepository = AuthRepository(db.userDao())

    /** RF-01: recuperação de senha por código enviado ao e-mail (EmailJS). */
    val passwordResetRepository = com.autotech.android.data.repository.PasswordResetRepository(
        context = application,
        userDao = db.userDao(),
        authRepository = authRepository,
        api = NetworkModule.emailJsApi(logDebug = BuildConfig.DEBUG),
        keys = com.autotech.android.data.remote.email.EmailJsKeys(
            serviceId = BuildConfig.EMAILJS_SERVICE_ID,
            templateId = BuildConfig.EMAILJS_TEMPLATE_ID,
            publicKey = BuildConfig.EMAILJS_PUBLIC_KEY,
            privateKey = BuildConfig.EMAILJS_PRIVATE_KEY
        )
    )

    val maintenanceRepository = MaintenanceRepository(
        maintenanceDao = db.maintenanceDao(),
        vehicleDao = db.vehicleDao(),
        cycleDao = db.maintenanceCycleDao(),
        pendingDao = db.pendingMaintenanceDao(),
        vehicleRepository = vehicleRepository
    )

    val fuelRepository = FuelRepository(
        fuelDao = db.fuelDao(),
        vehicleDao = db.vehicleDao(),
        vehicleRepository = vehicleRepository
    )
}
