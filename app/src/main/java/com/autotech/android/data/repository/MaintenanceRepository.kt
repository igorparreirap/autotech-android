package com.autotech.android.data.repository

import com.autotech.android.data.local.dao.MaintenanceCycleDao
import com.autotech.android.data.local.dao.MaintenanceDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.domain.OdometerRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MaintenanceRepository(
    private val maintenanceDao: MaintenanceDao,
    private val vehicleDao: VehicleDao,
    private val cycleDao: MaintenanceCycleDao,
    private val pendingDao: com.autotech.android.data.local.dao.PendingMaintenanceDao,
    private val vehicleRepository: VehicleRepository
) {

    fun observeRecords(vehicleId: Long): Flow<List<MaintenanceRecordEntity>> =
        maintenanceDao.observeByVehicle(vehicleId)

    /**
     * [updateVehicleOdometerTo]: KM atual do veículo, quando o usuário o informou ou marcou que o KM do
     * serviço é o atual; null quando não sabe (o odômetro não muda). RN-02 / RN-05.
     */
    suspend fun addRecord(
        record: MaintenanceRecordEntity,
        updateVehicleOdometerTo: Long?
    ): Result<Long> {
        val vehicle = vehicleDao.getById(record.vehicleId) ?: return Result.failure(IllegalStateException("Veículo não encontrado."))
        OdometerRules.validateAgainstOdometer(record.eventKm, vehicle.currentOdometerKm, updateVehicleOdometerTo).onFailure {
            return Result.failure(it)
        }
        if (updateVehicleOdometerTo != null) {
            OdometerRules.validateEventVsCurrent(record.eventKm, updateVehicleOdometerTo).onFailure { return Result.failure(it) }
            OdometerRules.validateNoOdometerRegression(updateVehicleOdometerTo, vehicleRepository.maxHistoricalKm(record.vehicleId))
                .onFailure { return Result.failure(it) }
        }
        OdometerRules.validateTimeline(record.eventDateMillis, record.eventKm, vehicleRepository.odometerEvents(record.vehicleId))
            .onFailure { return Result.failure(it) }
        val id = maintenanceDao.insert(record)
        // RF-06: registrar o serviço completa a pendência criada pelo "Já fiz" (se houver)
        pendingDao.deleteByItem(record.vehicleId, record.itemKey)
        if (updateVehicleOdometerTo != null) {
            val fresh = vehicleDao.getById(record.vehicleId)
            if (fresh != null && updateVehicleOdometerTo >= fresh.currentOdometerKm) {
                vehicleDao.update(fresh.copy(currentOdometerKm = updateVehicleOdometerTo))
            }
        }
        return Result.success(id)
    }

    suspend fun listForPdf(vehicleId: Long): List<MaintenanceRecordEntity> =
        maintenanceDao.listByVehicle(vehicleId)

    // ─── RF-06: "Já fiz" ─────────────────────────────────────────────────────

    fun observePendingByUser(userId: Long) = pendingDao.observeByUser(userId)

    fun observePending(vehicleId: Long) = pendingDao.observeByVehicle(vehicleId)

    suspend fun listPending(vehicleId: Long) = pendingDao.listByVehicle(vehicleId)

    suspend fun getPending(id: Long) = pendingDao.getById(id)

    /** Marca o item como feito na data informada; os dados do serviço são completados depois. */
    suspend fun markDone(vehicleId: Long, itemKey: String, doneDateMillis: Long): Long =
        pendingDao.upsert(
            com.autotech.android.data.local.entity.PendingMaintenanceEntity(
                vehicleId = vehicleId,
                itemKey = itemKey,
                doneDateMillis = doneDateMillis,
                createdAtMillis = System.currentTimeMillis()
            )
        )

    suspend fun undoPending(id: Long) = pendingDao.deleteById(id)

    /** Último ciclo configurado para o item, para pré-preencher o próximo registro. */
    suspend fun lastCycleFor(vehicleId: Long, itemKey: String): MaintenanceCycleEntity? {
        val dates = maintenanceDao.listByVehicle(vehicleId).associate { it.id to it.eventDateMillis }
        return cycleDao.observeByVehicle(vehicleId).first()
            .filter { it.itemKey == itemKey }
            .maxByOrNull { dates[it.linkedMaintenanceId] ?: 0L }
    }

    suspend fun addCustomCycle(
        vehicleId: Long,
        maintenanceId: Long,
        itemKey: String,
        intervalKm: Int?,
        intervalMonths: Int?
    ): Long {
        return cycleDao.insert(
            MaintenanceCycleEntity(
                vehicleId = vehicleId,
                linkedMaintenanceId = maintenanceId,
                itemKey = itemKey,
                intervalKm = intervalKm,
                intervalMonths = intervalMonths
            )
        )
    }

    fun observeCycles(vehicleId: Long): Flow<List<MaintenanceCycleEntity>> =
        cycleDao.observeByVehicle(vehicleId)
}
