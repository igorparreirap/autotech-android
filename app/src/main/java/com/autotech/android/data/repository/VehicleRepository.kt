package com.autotech.android.data.repository

import com.autotech.android.domain.VehicleNaming
import android.database.sqlite.SQLiteConstraintException
import com.autotech.android.data.local.dao.FuelDao
import com.autotech.android.data.local.dao.MaintenanceDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.VehicleEntity
import com.autotech.android.domain.OdometerRules
import com.autotech.android.domain.PlateRules
import kotlinx.coroutines.flow.Flow
import java.io.File

class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val maintenanceDao: MaintenanceDao,
    private val fuelDao: FuelDao
) {

    fun observeVehicles(userId: Long): Flow<List<VehicleEntity>> = vehicleDao.observeByUser(userId)

    suspend fun listVehicles(userId: Long): List<VehicleEntity> = vehicleDao.listByUser(userId)

    /** Todos os veículos, inclusive os excluídos que mantiveram o histórico. */
    suspend fun listAllVehicles(userId: Long): List<VehicleEntity> = vehicleDao.listAllByUser(userId)

    /**
     * RF-03: exclui o veículo.
     * [deleteHistory] = false: o veículo sai das listas, mas manutenções e abastecimentos
     * continuam guardados (aparecem no painel financeiro).
     * [deleteHistory] = true: apaga o veículo e, em cascata, manutenções, abastecimentos,
     * ciclos e os comprovantes anexados.
     */
    suspend fun deleteVehicle(vehicleId: Long, deleteHistory: Boolean): Result<Unit> = runCatching {
        if (deleteHistory) {
            val receipts = maintenanceDao.listByVehicle(vehicleId).mapNotNull { it.receiptPath } +
                fuelDao.listByVehicle(vehicleId).mapNotNull { it.receiptPath }
            vehicleDao.deleteById(vehicleId)
            receipts.forEach { runCatching { File(it).delete() } }
        } else {
            vehicleDao.archive(vehicleId)
        }
    }

    fun observeVehicle(id: Long): Flow<VehicleEntity?> = vehicleDao.observeById(id)

    suspend fun getVehicle(id: Long): VehicleEntity? = vehicleDao.getById(id)

    /** RN-05: quilometragens conhecidas por data (manutenções e abastecimentos já registrados). */
    suspend fun odometerEvents(vehicleId: Long): List<com.autotech.android.domain.OdometerEvent> =
        maintenanceDao.listByVehicle(vehicleId).map { com.autotech.android.domain.OdometerEvent(it.eventDateMillis, it.eventKm) } +
            fuelDao.listByVehicle(vehicleId).map { com.autotech.android.domain.OdometerEvent(it.dateMillis, it.eventKm) }

    suspend fun maxHistoricalKm(vehicleId: Long): Long {
        val vehicle = vehicleDao.getById(vehicleId) ?: return 0L
        val m1 = maintenanceDao.maxEventKm(vehicleId) ?: 0L
        val m2 = maintenanceDao.maxVehicleKmAtRecord(vehicleId) ?: 0L
        val f1 = fuelDao.maxEventKm(vehicleId) ?: 0L
        val f2 = fuelDao.maxVehicleKm(vehicleId) ?: 0L
        return maxOf(vehicle.currentOdometerKm, m1, m2, f1, f2)
    }

    suspend fun insertVehicle(entity: VehicleEntity): Result<Long> {
        OdometerRules.validateNoOdometerRegression(entity.currentOdometerKm, 0L).onFailure {
            return Result.failure(it)
        }
        val plate = PlateRules.normalize(entity.plate)
        // Placa de um veículo excluído com histórico mantido: reativa o mesmo cadastro.
        val archived = vehicleDao.findByUserAndPlate(entity.userId, plate)?.takeIf { it.archived }
        if (archived != null) {
            val maxKm = maxHistoricalKm(archived.id)
            OdometerRules.validateNoOdometerRegression(entity.currentOdometerKm, maxKm).onFailure {
                return Result.failure(it)
            }
            vehicleDao.update(entity.copy(id = archived.id, plate = plate, archived = false, nickname = archived.nickname))
            return Result.success(archived.id)
        }
        if (vehicleDao.countByUserAndPlate(entity.userId, plate, excludeId = 0L) > 0) {
            return Result.failure(IllegalArgumentException("Você já cadastrou um veículo com a placa $plate."))
        }
        return try {
            Result.success(vehicleDao.insert(entity.copy(id = 0, plate = plate)))
        } catch (e: SQLiteConstraintException) {
            Result.failure(
                IllegalStateException("Não foi possível salvar o veículo. Verifique se a placa já está cadastrada ou saia e entre novamente.")
            )
        }
    }

    /**
     * RF-03: renomeia o veículo. Nome vazio (ou igual ao original) apaga o apelido,
     * e o veículo volta a usar o nome da consulta da placa.
     */
    suspend fun renameVehicle(vehicleId: Long, nickname: String?): Result<Unit> = runCatching {
        val vehicle = vehicleDao.getById(vehicleId) ?: error("Veículo não encontrado.")
        val clean = VehicleNaming.normalizeNickname(nickname, vehicle.brand, vehicle.model)
        vehicleDao.setNickname(vehicleId, clean)
    }

    /** RF-03: combustível informado ou corrigido depois do cadastro. */
    suspend fun setFuelType(vehicleId: Long, type: com.autotech.android.domain.FuelType): Result<Unit> = runCatching {
        vehicleDao.setFuel(vehicleId, type.name)
    }

    suspend fun updateOdometer(vehicleId: Long, newKm: Long): Result<Unit> {
        val vehicle = vehicleDao.getById(vehicleId) ?: return Result.failure(IllegalStateException())
        val maxKm = maxHistoricalKm(vehicleId)
        OdometerRules.validateNoOdometerRegression(newKm, maxKm).onFailure { return Result.failure(it) }
        vehicleDao.update(vehicle.copy(currentOdometerKm = newKm))
        return Result.success(Unit)
    }

    suspend fun updateVehicle(entity: VehicleEntity): Result<Unit> {
        val plate = PlateRules.normalize(entity.plate)
        if (vehicleDao.countByUserAndPlate(entity.userId, plate, excludeId = entity.id) > 0) {
            return Result.failure(IllegalArgumentException("Você já cadastrou um veículo com a placa $plate."))
        }
        val maxKm = maxHistoricalKm(entity.id)
        OdometerRules.validateNoOdometerRegression(entity.currentOdometerKm, maxKm).onFailure {
            return Result.failure(it)
        }
        vehicleDao.update(entity.copy(plate = plate))
        return Result.success(Unit)
    }
}
