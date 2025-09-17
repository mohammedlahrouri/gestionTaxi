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