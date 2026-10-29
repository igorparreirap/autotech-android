package com.autotech.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autotech.android.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    // ABORT (e não REPLACE): com o índice único de placa, REPLACE apagaria o veículo
    // existente e, em cascata, todo o seu histórico de manutenções e abastecimentos.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vehicle: VehicleEntity): Long

    /** RF03: verifica se o usuário já possui outro veículo com esta placa. */
    @Query("SELECT COUNT(*) FROM vehicles WHERE userId = :userId AND plate = :plate AND id != :excludeId")
    suspend fun countByUserAndPlate(userId: Long, plate: String, excludeId: Long): Int

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND archived = 0 ORDER BY id DESC")
    fun observeByUser(userId: Long): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND archived = 0 ORDER BY id DESC")
    suspend fun listByUser(userId: Long): List<VehicleEntity>

    /** Inclui veículos excluídos com histórico mantido (usado no painel financeiro). */
    @Query("SELECT * FROM vehicles WHERE userId = :userId ORDER BY id DESC")
    suspend fun listAllByUser(userId: Long): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE userId = :userId AND plate = :plate LIMIT 1")
    suspend fun findByUserAndPlate(userId: Long, plate: String): VehicleEntity?

    @Query("UPDATE vehicles SET nickname = :nickname WHERE id = :id")
    suspend fun setNickname(id: Long, nickname: String?)

    @Query("UPDATE vehicles SET fuel = :fuel WHERE id = :id")
    suspend fun setFuel(id: Long, fuel: String)

    @Query("UPDATE vehicles SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    /** Apaga o veículo; manutenções, abastecimentos e ciclos saem junto (ON DELETE CASCADE). */
    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<VehicleEntity?>
}
