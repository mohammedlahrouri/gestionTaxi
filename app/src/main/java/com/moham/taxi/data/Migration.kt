package com.moham.taxi.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2 of the database.
 * Adds new indices for optimized queries.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add compound index for date + paymentMethod in taxi_rides
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_date_paymentMethod` ON `taxi_rides` (`date`, `paymentMethod`)")
        
        // Add index for price in taxi_rides
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_price` ON `taxi_rides` (`price`)")
        
        // Add compound index for date + price in taxi_rides
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_date_price` ON `taxi_rides` (`date`, `price`)")
        
        // Add compound index for date + type in expenses
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date_type` ON `expenses` (`date`, `type`)")
        
        // Add index for amount in expenses
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_amount` ON `expenses` (`amount`)")
        
        // Add compound index for date + amount in expenses
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date_amount` ON `expenses` (`date`, `amount`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `serviceType` TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `servicePlatform` TEXT DEFAULT NULL")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `service_platforms` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("INSERT INTO `service_platforms` (`name`) VALUES ('Directo')")
        database.execSQL("INSERT INTO `service_platforms` (`name`) VALUES ('FreeNow')")
        database.execSQL("INSERT INTO `service_platforms` (`name`) VALUES ('Cabify')")
        database.execSQL("INSERT INTO `service_platforms` (`name`) VALUES ('Uber')")
        database.execSQL("INSERT INTO `service_platforms` (`name`) VALUES ('Bolt')")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_servicePlatform` ON `taxi_rides` (`servicePlatform`)")
        database.execSQL("INSERT OR IGNORE INTO `payment_methods` (`name`) VALUES ('Via App')")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `rideTime` TEXT NOT NULL DEFAULT '00:00'")
        database.execSQL("UPDATE `taxi_rides` SET `servicePlatform` = 'Directo' WHERE `servicePlatform` IS NULL OR `servicePlatform` = ''")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `service_platforms` ADD COLUMN `commissionPercentage` REAL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `netPrice` REAL")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `commissionPercentAtTime` REAL")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `service_platforms` ADD COLUMN `commissionVat` REAL")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `commissionVatAtTime` REAL")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `service_platforms` ADD COLUMN `useAlternativeMath` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `maintenanceKilometers` INTEGER")
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `maintenanceDetails` TEXT")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tariffs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `isFixed` INTEGER NOT NULL,
                `fixedPrice` REAL,
                `baseFare` REAL,
                `pricePerKm` REAL,
                `surcharge` REAL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `surcharges` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `price` REAL NOT NULL,
                `isPerItem` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `platform_payment_methods` (
                `platformId` INTEGER NOT NULL,
                `paymentMethodId` INTEGER NOT NULL,
                PRIMARY KEY(`platformId`, `paymentMethodId`),
                FOREIGN KEY(`platformId`) REFERENCES `service_platforms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`paymentMethodId`) REFERENCES `payment_methods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_platform_payment_methods_platformId` ON `platform_payment_methods` (`platformId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_platform_payment_methods_paymentMethodId` ON `platform_payment_methods` (`paymentMethodId`)")

        database.execSQL(
            """
            INSERT INTO `service_platforms` (`name`, `commissionPercentage`, `commissionVat`, `useAlternativeMath`)
            SELECT 'Directo', NULL, NULL, 0
            WHERE NOT EXISTS (
                SELECT 1 FROM `service_platforms` WHERE lower(`name`) = 'directo'
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO `payment_methods` (`name`)
            SELECT DISTINCT tr.`paymentMethod`
            FROM `taxi_rides` tr
            WHERE tr.`paymentMethod` IS NOT NULL
              AND tr.`paymentMethod` != ''
              AND NOT EXISTS (
                  SELECT 1 FROM `payment_methods` pm WHERE lower(pm.`name`) = lower(tr.`paymentMethod`)
              )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT OR IGNORE INTO `platform_payment_methods` (`platformId`, `paymentMethodId`)
            SELECT sp.`id`, pm.`id`
            FROM `service_platforms` sp
            CROSS JOIN `payment_methods` pm
            """.trimIndent()
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `service_platforms` ADD COLUMN `serviceMode` TEXT NOT NULL DEFAULT 'BOTH'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `tip` REAL")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `workingDate` INTEGER")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `paymentMethodId` INTEGER")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `servicePlatformId` INTEGER")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `tariffId` INTEGER")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `rideDurationMinutes` INTEGER")
        
        // Copiamos la fecha que ya estaba en 'date' (que en la app vieja se usaba como Jornada) a 'workingDate'
        database.execSQL("UPDATE `taxi_rides` SET `workingDate` = `date`")
        
        // Vinculamos paymentMethod y servicePlatform existentes a sus IDs
        database.execSQL("UPDATE `taxi_rides` SET `paymentMethodId` = (SELECT `id` FROM `payment_methods` WHERE lower(`name`) = lower(`taxi_rides`.`paymentMethod`))")
        database.execSQL("UPDATE `taxi_rides` SET `servicePlatformId` = (SELECT `id` FROM `service_platforms` WHERE lower(`name`) = lower(`taxi_rides`.`servicePlatform`))")

        // Creamos índices para optimización de claves foráneas y búsquedas
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_workingDate` ON `taxi_rides` (`workingDate`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_paymentMethodId` ON `taxi_rides` (`paymentMethodId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_servicePlatformId` ON `taxi_rides` (`servicePlatformId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_taxi_rides_tariffId` ON `taxi_rides` (`tariffId`)")

        // Creamos la tabla intermedia para los suplementos
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ride_surcharge_cross_ref` (
                `rideId` INTEGER NOT NULL,
                `surchargeId` INTEGER NOT NULL,
                `count` INTEGER NOT NULL,
                PRIMARY KEY(`rideId`, `surchargeId`),
                FOREIGN KEY(`rideId`) REFERENCES `taxi_rides`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`surchargeId`) REFERENCES `surcharges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_ride_surcharge_cross_ref_surchargeId` ON `ride_surcharge_cross_ref` (`surchargeId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_ride_surcharge_cross_ref_rideId` ON `ride_surcharge_cross_ref` (`rideId`)")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `ticketPhotoPath` TEXT")
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `ticketPhotoPath` TEXT")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `realDate` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE `taxi_rides` SET `realDate` = `date` WHERE `realDate` = 0")
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `realDate` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE `expenses` SET `realDate` = `date` WHERE `realDate` = 0")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `taxi_rides` ADD COLUMN `firestoreId` TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `firestoreId` TEXT DEFAULT NULL")
    }
}

