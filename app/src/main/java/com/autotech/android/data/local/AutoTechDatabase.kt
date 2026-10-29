package com.autotech.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autotech.android.data.local.dao.FuelDao
import com.autotech.android.data.local.dao.MaintenanceCycleDao
import com.autotech.android.data.local.dao.MaintenanceDao
import com.autotech.android.data.local.dao.PendingMaintenanceDao
import com.autotech.android.data.local.entity.PendingMaintenanceEntity
import com.autotech.android.data.local.dao.PlateLookupCacheDao
import com.autotech.android.data.local.dao.UserDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.PlateLookupCacheEntity
import com.autotech.android.data.local.entity.UserEntity
import com.autotech.android.data.local.entity.VehicleEntity

@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        MaintenanceRecordEntity::class,
        FuelRecordEntity::class,
        MaintenanceCycleEntity::class,
        PlateLookupCacheEntity::class,
        PendingMaintenanceEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AutoTechDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun fuelDao(): FuelDao
    abstract fun maintenanceCycleDao(): MaintenanceCycleDao
    abstract fun plateLookupCacheDao(): PlateLookupCacheDao
    abstract fun pendingMaintenanceDao(): PendingMaintenanceDao

    companion object {
        /** v8 -> v9: valor da mão de obra separado na manutenção (RF-04). */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE maintenance_records ADD COLUMN laborAmount REAL")
            }
        }

        /** v7 -> v8: manutenções marcadas como feitas ("Já fiz") aguardando os dados (RF-06). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_maintenance` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, " +
                        "`itemKey` TEXT NOT NULL, `doneDateMillis` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_maintenance_vehicleId` ON `pending_maintenance` (`vehicleId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_maintenance_vehicleId_itemKey` ON `pending_maintenance` (`vehicleId`, `itemKey`)")
            }
        }

        /** v6 -> v7: litros de álcool/gasolina e cálculo de consumo no abastecimento (RF-05). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_records ADD COLUMN ethanolLiters REAL")
                db.execSQL("ALTER TABLE fuel_records ADD COLUMN gasolineLiters REAL")
                db.execSQL("ALTER TABLE fuel_records ADD COLUMN computeConsumption INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v5 -> v6: nome personalizado do veículo (RF-03). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN nickname TEXT")
            }
        }

        /** v4 -> v5: exclusão de veículo mantendo o histórico (RF-03). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3 -> v4: data do registro em manutenções e abastecimentos (RF-04 / RF-05).
         * Os registros antigos recebem a própria data do evento como data do registro.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE maintenance_records ADD COLUMN registeredAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE maintenance_records SET registeredAtMillis = eventDateMillis")
                db.execSQL("ALTER TABLE fuel_records ADD COLUMN registeredAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE fuel_records SET registeredAtMillis = dateMillis")
            }
        }

        fun build(context: Context): AutoTechDatabase =
            Room.databaseBuilder(context, AutoTechDatabase::class.java, "autotech.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigration()
                .build()
    }
}
