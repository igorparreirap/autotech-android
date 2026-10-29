package com.autotech.android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * RF-06: manutenção que o usuário disse que já fez ("Já fiz" na notificação ou no app), mas
 * ainda sem quilometragem, valor, descrição e comprovante. Enquanto estiver pendente, o item não
 * gera novos avisos; ao completar o registro, a pendência é apagada e começa um novo ciclo.
 */
@Entity(
    tableName = "pending_maintenance",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index(value = ["vehicleId", "itemKey"], unique = true)]
)
data class PendingMaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val itemKey: String,
    /** Data em que o serviço foi feito (a data da notificação, ou o dia em que marcou no app). */
    val doneDateMillis: Long,
    val createdAtMillis: Long
)
