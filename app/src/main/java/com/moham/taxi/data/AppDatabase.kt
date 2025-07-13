package com.moham.taxi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moham.taxi.data.dao.ExpenseDao
import com.moham.taxi.data.dao.PaymentMethodDao
import com.moham.taxi.data.dao.TaxiRideDao
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.MIGRATION_1_2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaxiRide::class, Expense::class, PaymentMethod::class],
    version = 2, // Incremented version for new indices
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taxiRideDao(): TaxiRideDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentMethodDao(): PaymentMethodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taxi_app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // Keep as fallback for other migrations
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Inicializa los métodos de pago predeterminados
                        val paymentMethodDao = database.paymentMethodDao()
                        
                        val defaultPaymentMethods = listOf(
                            PaymentMethod(name = "Tarjeta"),
                            PaymentMethod(name = "Efectivo"),
                            PaymentMethod(name = "Uber"),
                            PaymentMethod(name = "Frenow")
                        )
                        
                        defaultPaymentMethods.forEach { paymentMethodDao.insert(it) }
                    }
                }
            }
        }
    }
}
