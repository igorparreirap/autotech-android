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
import com.autotech.android.data.local.dao.ReceiptDao
import com.autotech.android.data.local.dao.UserDao
import com.autotech.android.data.local.dao.VehicleDao
import com.autotech.android.data.local.entity.FuelRecordEntity
import com.autotech.android.data.local.entity.MaintenanceCycleEntity
import com.autotech.android.data.local.entity.MaintenanceRecordEntity
import com.autotech.android.data.local.entity.PlateLookupCacheEntity
import com.autotech.android.data.local.entity.ReceiptEntity
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
        PendingMaintenanceEntity::class,
        ReceiptEntity::class
    ],
    version = 10,
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
    abstract fun receiptDao(): ReceiptDao

    companion object {
        /**
         * v9 -> v10:
         * - valores em dinheiro passam de REAL (reais) para INTEGER (centavos): amount -> amountCents,
         *   laborAmount -> laborCents (RF-04 / RF-05 / RF-08);
         * - os comprovantes saem do campo receiptPath (vários caminhos, um por linha) e vão para a
         *   tabela receipts, uma linha por arquivo.
         * O SQLite das versões antigas do Android não remove colunas, então as duas tabelas são recriadas.
         * Durante a migração as chaves estrangeiras não estão ativas (o Room as liga ao abrir o banco);
         * mesmo assim, os ciclos são copiados antes e devolvidos se algum se perder ao recriar a tabela pai.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // comprovantes no formato antigo, lidos antes de recriar as tabelas
                val maintReceipts = mutableListOf<Pair<Long, List<String>>>()
                db.query("SELECT id, receiptPath FROM maintenance_records WHERE receiptPath IS NOT NULL").use { c ->
                    while (c.moveToNext()) maintReceipts += c.getLong(0) to com.autotech.android.domain.ReceiptPaths.split(c.getString(1))
                }
                val fuelReceipts = mutableListOf<Pair<Long, List<String>>>()
                db.query("SELECT id, receiptPath FROM fuel_records WHERE receiptPath IS NOT NULL").use { c ->
                    while (c.moveToNext()) fuelReceipts += c.getLong(0) to com.autotech.android.domain.ReceiptPaths.split(c.getString(1))
                }
                db.execSQL("CREATE TABLE `_bkp_cycles` AS SELECT * FROM `maintenance_cycles`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_new_maintenance_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `itemKey` TEXT NOT NULL, " +
                        "`eventDateMillis` INTEGER NOT NULL, `eventKm` INTEGER NOT NULL, `vehicleKmAtRecord` INTEGER NOT NULL, " +
                        "`amountCents` INTEGER, `description` TEXT, `registeredAtMillis` INTEGER NOT NULL DEFAULT 0, `laborCents` INTEGER, " +
                        "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "INSERT INTO `_new_maintenance_records` (id, vehicleId, itemKey, eventDateMillis, eventKm, vehicleKmAtRecord, " +
                        "amountCents, description, registeredAtMillis, laborCents) " +
                        "SELECT id, vehicleId, itemKey, eventDateMillis, eventKm, vehicleKmAtRecord, " +
                        "CAST(ROUND(amount * 100) AS INTEGER), description, registeredAtMillis, CAST(ROUND(laborAmount * 100) AS INTEGER) " +
                        "FROM `maintenance_records`"
                )
                db.execSQL("DROP TABLE `maintenance_records`")
                db.execSQL("ALTER TABLE `_new_maintenance_records` RENAME TO `maintenance_records`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId` ON `maintenance_records` (`vehicleId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_new_fuel_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, " +
                        "`eventKm` INTEGER NOT NULL, `vehicleKm` INTEGER NOT NULL, `liters` REAL NOT NULL, `amountCents` INTEGER, " +
                        "`registeredAtMillis` INTEGER NOT NULL DEFAULT 0, `ethanolLiters` REAL, `gasolineLiters` REAL, " +
                        "`computeConsumption` INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "INSERT INTO `_new_fuel_records` (id, vehicleId, dateMillis, eventKm, vehicleKm, liters, amountCents, " +
                        "registeredAtMillis, ethanolLiters, gasolineLiters, computeConsumption) " +
                        "SELECT id, vehicleId, dateMillis, eventKm, vehicleKm, liters, CAST(ROUND(amount * 100) AS INTEGER), " +
                        "registeredAtMillis, ethanolLiters, gasolineLiters, computeConsumption FROM `fuel_records`"
                )
                db.execSQL("DROP TABLE `fuel_records`")
                db.execSQL("ALTER TABLE `_new_fuel_records` RENAME TO `fuel_records`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_records_vehicleId` ON `fuel_records` (`vehicleId`)")

                // ciclos que se perderam ao recriar a tabela pai voltam (os ids são os mesmos)
                db.execSQL("INSERT OR IGNORE INTO `maintenance_cycles` SELECT * FROM `_bkp_cycles`")
                db.execSQL("DROP TABLE `_bkp_cycles`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `receipts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `maintenanceRecordId` INTEGER, `fuelRecordId` INTEGER, " +
                        "`path` TEXT NOT NULL, `position` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`maintenanceRecordId`) REFERENCES `maintenance_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`fuelRecordId`) REFERENCES `fuel_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipts_maintenanceRecordId` ON `receipts` (`maintenanceRecordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipts_fuelRecordId` ON `receipts` (`fuelRecordId`)")
                val now = System.currentTimeMillis()
                for ((id, paths) in maintReceipts) paths.forEachIndexed { i, p ->
                    db.execSQL(
                        "INSERT INTO receipts (maintenanceRecordId, fuelRecordId, path, position, createdAtMillis) VALUES (?, NULL, ?, ?, ?)",
                        arrayOf<Any>(id, p, i, now)
                    )
                }
                for ((id, paths) in fuelReceipts) paths.forEachIndexed { i, p ->
                    db.execSQL(
                        "INSERT INTO receipts (maintenanceRecordId, fuelRecordId, path, position, createdAtMillis) VALUES (NULL, ?, ?, ?, ?)",
                        arrayOf<Any>(id, p, i, now)
                    )
                }
            }
        }

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

        /** v6 -> v7: litros de etanol/gasolina e cálculo de consumo no abastecimento (RF-05). */
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
    }
}
