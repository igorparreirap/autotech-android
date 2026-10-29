package com.autotech.android.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val VEHICLES = "vehicles"
    const val VEHICLE_DETAIL = "vehicle/{vehicleId}?pdf={pdf}"
    const val VEHICLE_ADD = "vehicle/add"
    const val MAINTENANCE_LIST = "maintenance/{vehicleId}"
    const val MAINTENANCE_ADD = "maintenance/{vehicleId}/add?pending={pending}"
    const val FUEL_LIST = "fuel/{vehicleId}"
    const val FUEL_ADD = "fuel/{vehicleId}/add"
    const val DASHBOARD = "dashboard"
    const val MAP = "map"
    const val NOTIFICATIONS = "notifications"
    const val SEARCH = "search"
    const val FORGOT_PASSWORD = "forgot?email={email}"
    const val PDF_VIEW = "pdf?path={path}"

    fun vehicleDetail(id: Long, openPdf: Boolean = false) = if (openPdf) "vehicle/$id?pdf=true" else "vehicle/$id"
    fun maintenanceList(vehicleId: Long) = "maintenance/$vehicleId"
    /** [pendingId]: completa uma manutenção marcada como "Já fiz" (RF-06). */
    fun maintenanceAdd(vehicleId: Long, pendingId: Long? = null) =
        if (pendingId != null) "maintenance/$vehicleId/add?pending=$pendingId" else "maintenance/$vehicleId/add"
    fun fuelList(vehicleId: Long) = "fuel/$vehicleId"
    fun fuelAdd(vehicleId: Long) = "fuel/$vehicleId/add"
    fun forgotPassword(email: String) = "forgot?email=${android.net.Uri.encode(email)}"
    fun pdfView(path: String) = "pdf?path=${android.net.Uri.encode(path)}"
}
