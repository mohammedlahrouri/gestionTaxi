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
import com.moham.taxi.data.dao.PendingDeletionDao
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.PendingDeletion
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
import com.moham.taxi.data.MIGRATION_18_19
import com.moham.taxi.data.MIGRATION_19_20
import com.moham.taxi.data.MIGRATION_20_21
import com.moham.taxi.data.MIGRATION_21_22
import com.moham.taxi.data.MIGRATION_22_23
import com.moham.taxi.data.MIGRATION_23_24
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [TaxiRide::class, Expense::class, PaymentMethod::class, ServicePlatform::class, Tariff::class, Surcharge::class, PlatformPaymentMethodCrossRef::class, RideSurchargeCrossRef::class, PendingDeletion::class],
    version = 24,
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
    abstract fun pendingDeletionDao(): PendingDeletionDao

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
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                        MIGRATION_23_24
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            private val initMutex = Mutex()

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // onCreate no inserta payment_methods para evitar condición de carrera con onOpen.
                // Toda la inicialización y saneamiento se gestiona centralizadamente en onOpen.
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        initMutex.withLock {
                            try {
                                val paymentMethodDao = database.paymentMethodDao()
                                val platformPaymentMethodDao = database.platformPaymentMethodDao()
                                val servicePlatformDao = database.servicePlatformDao()

                                // 1. Asegurar que existe al menos la plataforma Directo
                                var allPlatforms = servicePlatformDao.getAllServicePlatformsList()
                                if (allPlatforms.isEmpty()) {
                                    servicePlatformDao.insert(ServicePlatform(name = "Directo"))
                                    allPlatforms = servicePlatformDao.getAllServicePlatformsList()
                                }
                                val directPlatform = allPlatforms.firstOrNull { it.name.trim().equals("Directo", ignoreCase = true) } ?: allPlatforms.firstOrNull()

                                // 2. Limpieza de duplicados existentes en payment_methods
                                val currentMethods = paymentMethodDao.getAllPaymentMethodsList()
                                val groupedByName = currentMethods.groupBy { m ->
                                    val lower = m.name.trim().lowercase().replace(" ", "").replace("á", "a").replace("í", "i")
                                    when {
                                        lower == "efectivo" || lower == "cash" -> "efectivo"
                                        lower == "tarjeta" || lower == "card" -> "tarjeta"
                                        lower == "viaapp" -> "viaapp"
                                        lower in listOf("cancelado", "cancelada", "rechazado") -> "cancelado"
                                        else -> lower
                                    }
                                }

                                for ((_, group) in groupedByName) {
                                    if (group.size > 1) {
                                        // Conservar el registro original (menor ID)
                                        val keep = group.minByOrNull { it.id } ?: group.first()
                                        val duplicates = group.filter { it.id != keep.id }
                                        for (dup in duplicates) {
                                            // Reasignar carreras del duplicado hacia el ID que se conserva
                                            db.execSQL(
                                                "UPDATE `taxi_rides` SET `paymentMethodId` = ? WHERE `paymentMethodId` = ?",
                                                arrayOf<Any>(keep.id, dup.id)
                                            )
                                            // Eliminar enlaces del duplicado en platform_payment_methods
                                            db.execSQL(
                                                "DELETE FROM `platform_payment_methods` WHERE `paymentMethodId` = ?",
                                                arrayOf<Any>(dup.id)
                                            )
                                            // Eliminar el método de pago duplicado
                                            paymentMethodDao.delete(dup)
                                        }
                                    }
                                }

                                // 3. Asegurar que los 4 métodos estándar existen
                                val methodsAfterCleanup = paymentMethodDao.getAllPaymentMethodsList()
                                val defaultNames = listOf(
                                    PaymentMethod.METHOD_CASH,
                                    PaymentMethod.METHOD_CARD,
                                    PaymentMethod.METHOD_APP,
                                    PaymentMethod.METHOD_CANCELLED
                                )

                                for (defName in defaultNames) {
                                    val existing = methodsAfterCleanup.firstOrNull { m ->
                                        val n = m.name.trim().lowercase().replace(" ", "").replace("á", "a").replace("í", "i")
                                        val target = defName.trim().lowercase().replace(" ", "").replace("á", "a").replace("í", "i")
                                        n == target || (target == "cancelado" && n in listOf("cancelado", "cancelada", "rechazado"))
                                    }

                                    val methodId = if (existing == null) {
                                        paymentMethodDao.insert(PaymentMethod(name = defName))
                                    } else {
                                        existing.id
                                    }

                                    if (methodId > 0) {
                                        if (defName == PaymentMethod.METHOD_CANCELLED) {
                                            if (allPlatforms.isNotEmpty()) {
                                                val refs = allPlatforms.map { platform ->
                                                    PlatformPaymentMethodCrossRef(platformId = platform.id, paymentMethodId = methodId)
                                                }
                                                platformPaymentMethodDao.insertAll(refs)
                                            }
                                        } else if (directPlatform != null) {
                                            platformPaymentMethodDao.insertAll(listOf(
                                                PlatformPaymentMethodCrossRef(platformId = directPlatform.id, paymentMethodId = methodId)
                                            ))
                                        }
                                    }
                                }

                                // 4. Asegurar tarifas iniciales si no existen
                                val tariffDao = database.tariffDao()
                                if (tariffDao.getCount() == 0) {
                                    tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Tarifa 1", isFixed = false, baseFare = 2.50, pricePerKm = 1.30, surcharge = 0.0))
                                    tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Tarifa 2", isFixed = false, baseFare = 3.15, pricePerKm = 1.50, surcharge = 0.0))
                                    tariffDao.insert(com.moham.taxi.data.model.Tariff(name = "Aeropuerto", isFixed = true, fixedPrice = 33.0))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
