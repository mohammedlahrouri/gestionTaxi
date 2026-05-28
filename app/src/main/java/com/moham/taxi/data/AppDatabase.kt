package com.moham.taxi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moham.taxi.data.dao.ExpenseDao
import com.moham.taxi.data.dao.PlatformPaymentMethodDao
import com.moham.taxi.data.dao.PaymentMethodDao
import com.moham.taxi.data.dao.ServicePlatformDao
import com.moham.taxi.data.dao.TaxiRideDao
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.PlatformPaymentMethodCrossRef
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.data.model.Surcharge
import com.moham.taxi.data.model.RideSurchargeCrossRef
import com.moham.taxi.data.MIGRATION_1_2
import com.moham.taxi.data.MIGRATION_2_3
import com.moham.taxi.data.MIGRATION_3_4
import com.moham.taxi.data.MIGRATION_4_5
import com.moham.taxi.data.MIGRATION_5_6
import com.moham.taxi.data.MIGRATION_6_7
import com.moham.taxi.data.MIGRATION_7_8
import com.moham.taxi.data.MIGRATION_8_9
import com.moham.taxi.data.MIGRATION_9_10
import com.moham.taxi.data.MIGRATION_10_11
import com.moham.taxi.data.MIGRATION_11_12
import com.moham.taxi.data.MIGRATION_12_13
import com.moham.taxi.data.MIGRATION_13_14
import com.moham.taxi.data.MIGRATION_14_15
import com.moham.taxi.data.MIGRATION_15_16
import com.moham.taxi.data.MIGRATION_16_17
import com.moham.taxi.data.MIGRATION_17_18
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaxiRide::class, Expense::class, PaymentMethod::class, ServicePlatform::class, Tariff::class, Surcharge::class, PlatformPaymentMethodCrossRef::class, RideSurchargeCrossRef::class],
    version = 18,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taxiRideDao(): TaxiRideDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun servicePlatformDao(): ServicePlatformDao
    abstract fun platformPaymentMethodDao(): PlatformPaymentMethodDao
    abstract fun tariffDao(): com.moham.taxi.data.dao.TariffDao
    abstract fun surchargeDao(): com.moham.taxi.data.dao.SurchargeDao

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18
                    )
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
                        val paymentMethodDao = database.paymentMethodDao()
                        val servicePlatformDao = database.servicePlatformDao()
                        val platformPaymentMethodDao = database.platformPaymentMethodDao()
                        
                        val defaultPaymentMethods = listOf(
                            PaymentMethod(name = "Efectivo"),
                            PaymentMethod(name = "Tarjeta")
                        )

                        val paymentMethodIds = defaultPaymentMethods.map { paymentMethodDao.insert(it) }
                        val directPlatformId = servicePlatformDao.insert(ServicePlatform(name = "Directo"))
                        platformPaymentMethodDao.insertAll(
                            paymentMethodIds.map { methodId ->
                                PlatformPaymentMethodCrossRef(platformId = directPlatformId, paymentMethodId = methodId)
                            }
                        )

                        val tariffDao = database.tariffDao()
                        if (tariffDao.getCount() == 0) {
                            tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Tarifa 1", isFixed = false, baseFare = 2.50, pricePerKm = 1.30, surcharge = 0.0))
                            tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Tarifa 2", isFixed = false, baseFare = 3.15, pricePerKm = 1.50, surcharge = 0.0))
                            tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Aeropuerto", isFixed = true, fixedPrice = 33.0))
                        }
                    }
                }
            }
        }
    }
}
