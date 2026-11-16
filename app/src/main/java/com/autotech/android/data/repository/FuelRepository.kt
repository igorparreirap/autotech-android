package com.autotech.android.data.repository

import com.autotech.android.data.local.dao.FuelDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.domain.OdometerRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FuelRepository(
    private val fuelDao: FuelDao,
    private val vehicleDao: VehicleDao,
    private val receiptDao: com.autotech.android.data.local.dao.ReceiptDao,
    private val vehicleRepository: VehicleRepository
) {

    fun observeRecords(vehicleId: Long): Flow<List<FuelRecordEntity>> =
        fuelDao.observeByVehicle(vehicleId)

    /** RF-05: comprovantes de cada abastecimento do veículo (id do registro -> caminhos, na ordem anexada). */
    fun observeReceipts(vehicleId: Long): Flow<Map<Long, List<String>>> =
        receiptDao.observeFuelByVehicle(vehicleId).map { l -> l.groupBy({ it.fuelRecordId ?: 0L }, { it.path }) }

    suspend fun listReceipts(vehicleId: Long): Map<Long, List<String>> =
        receiptDao.listFuelByVehicle(vehicleId).groupBy({ it.fuelRecordId ?: 0L }, { it.path })

    /** [updateVehicleOdometerTo]: KM atual do veículo, ou null quando o usuário não sabe (RN-02 / RN-05). */
    suspend fun addRecord(
        record: FuelRecordEntity,
        updateVehicleOdometerTo: Long?,
        receipts: List<String> = emptyList()
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
        // RF-05: um registro na tabela receipts por arquivo anexado
        if (receipts.isNotEmpty()) {
            val now = System.currentTimeMillis()
            receiptDao.insertAll(receipts.mapIndexed { i, p -> com.autotech.android.data.local.entity.ReceiptEntity(maintenanceRecordId = null, fuelRecordId = id, path = p, position = i, createdAtMillis = now) })
        }
        if (updateVehicleOdometerTo != null && updateVehicleOdometerTo >= vehicle.currentOdometerKm) {
            vehicleDao.update(vehicle.copy(currentOdometerKm = updateVehicleOdometerTo))
        }
        return Result.success(id)
    }

    suspend fun listForVehicle(vehicleId: Long): List<FuelRecordEntity> =
        fuelDao.listByVehicle(vehicleId)
}
