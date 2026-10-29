package com.autotech.android.data.repository

import com.autotech.android.data.local.dao.FuelDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.domain.OdometerRules
import kotlinx.coroutines.flow.Flow

class FuelRepository(
    private val fuelDao: FuelDao,
    private val vehicleDao: VehicleDao,
    private val vehicleRepository: VehicleRepository
) {

    fun observeRecords(vehicleId: Long): Flow<List<FuelRecordEntity>> =
        fuelDao.observeByVehicle(vehicleId)

    /** [updateVehicleOdometerTo]: KM atual do veículo, ou null quando o usuário não sabe (RN-02 / RN-05). */
    suspend fun addRecord(
        record: FuelRecordEntity,
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
        OdometerRules.validateTimeline(record.dateMillis, record.eventKm, vehicleRepository.odometerEvents(record.vehicleId))
            .onFailure { return Result.failure(it) }
        val id = fuelDao.insert(record)
        if (updateVehicleOdometerTo != null && updateVehicleOdometerTo >= vehicle.currentOdometerKm) {
            vehicleDao.update(vehicle.copy(currentOdometerKm = updateVehicleOdometerTo))
        }
        return Result.success(id)
    }

    suspend fun listForVehicle(vehicleId: Long): List<FuelRecordEntity> =
        fuelDao.listByVehicle(vehicleId)
}
