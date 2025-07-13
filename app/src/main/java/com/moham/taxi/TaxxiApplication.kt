package com.moham.taxi

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.PaymentMethodRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import com.moham.taxi.data.service.ExportService
import com.moham.taxi.data.service.BackupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date

// Extension property para DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taxi_settings")

class GestionTaxiApplication : Application() {
    
    // Scope único optimizado para todas las operaciones
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Sistema de caché para preferencias frecuentemente usadas
    private var cachedFirstDayOfWeek: Int? = null
    private var cachedSummaryIncomeType: Int? = null
    private var cachedSelectedDate: Date? = null
    private var cachedBillingData: BillingData? = null
    
    // Base de datos y DAOs
    private val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    private val taxiRideDao by lazy { database.taxiRideDao() }
    private val expenseDao by lazy { database.expenseDao() }
    private val paymentMethodDao by lazy { database.paymentMethodDao() }
    
    // Repositorios
    val taxiRideRepository by lazy { TaxiRideRepository(taxiRideDao, database, this) }
    val expenseRepository by lazy { ExpenseRepository(expenseDao, database, this) }
    val paymentMethodRepository by lazy { PaymentMethodRepository(paymentMethodDao, database) }
    
    // Servicio de exportación
    val exportService by lazy { ExportService(this, taxiRideRepository, expenseRepository) }
    
    // Servicio de backup
    val backupService by lazy { BackupService(this, taxiRideRepository, expenseRepository, paymentMethodRepository, database) }
    
    // Claves para almacenar preferencias
    companion object {
        val SELECTED_DATE_KEY = longPreferencesKey("selected_date")
        val FIRST_DAY_OF_WEEK_KEY = intPreferencesKey("first_day_of_week")
        val SUMMARY_INCOME_TYPE_KEY = intPreferencesKey("summary_income_type")
        val BILLING_NAME_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_name")
        val BILLING_NIF_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_nif")
        val BILLING_LICENSE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_license")
        val BILLING_STREET_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_street")
        val BILLING_CITY_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_city")
        val BILLING_POSTAL_CODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("billing_postal_code")
    }
    
    // Método para guardar la fecha seleccionada con caché
    suspend fun saveSelectedDate(date: Date) {
        dataStore.edit { preferences ->
            preferences[SELECTED_DATE_KEY] = date.time
        }
        cachedSelectedDate = date
    }
    
    // Método para obtener la fecha seleccionada con caché
    fun getSelectedDate(): Flow<Date> {
        return dataStore.data.map { preferences ->
            cachedSelectedDate ?: preferences[SELECTED_DATE_KEY]?.let { timestamp ->
                Date(timestamp).also { date ->
                    cachedSelectedDate = date
                }
            } ?: Date().also { date ->
                cachedSelectedDate = date
                applicationScope.launch {
                    saveSelectedDate(date)
                }
            }
        }
    }
    
    // Método para guardar el primer día de la semana con caché
    suspend fun saveFirstDayOfWeek(dayOfWeek: Int) {
        dataStore.edit { preferences ->
            preferences[FIRST_DAY_OF_WEEK_KEY] = dayOfWeek
        }
        cachedFirstDayOfWeek = dayOfWeek
    }
    
    // Método para obtener el primer día de la semana con caché
    fun getFirstDayOfWeek(): Flow<Int> {
        return dataStore.data.map { preferences ->
            cachedFirstDayOfWeek ?: preferences[FIRST_DAY_OF_WEEK_KEY]?.also {
                cachedFirstDayOfWeek = it
            } ?: 2
        }
    }
    
    // Método para guardar el tipo de ingreso con caché
    suspend fun saveSummaryIncomeType(type: Int) {
        dataStore.edit { preferences ->
            preferences[SUMMARY_INCOME_TYPE_KEY] = type
        }
        cachedSummaryIncomeType = type
    }
    
    // Método para obtener el tipo de ingreso con caché
    fun getSummaryIncomeType(): Flow<Int> {
        return dataStore.data.map { preferences ->
            cachedSummaryIncomeType ?: preferences[SUMMARY_INCOME_TYPE_KEY]?.also {
                cachedSummaryIncomeType = it
            } ?: 0
        }
    }
    
    // Método para guardar datos de facturación con caché
    suspend fun saveBillingData(
        name: String,
        nif: String,
        license: String,
        street: String,
        city: String,
        postalCode: String
    ) {
        dataStore.edit { preferences ->
            preferences[BILLING_NAME_KEY] = name
            preferences[BILLING_NIF_KEY] = nif
            preferences[BILLING_LICENSE_KEY] = license
            preferences[BILLING_STREET_KEY] = street
            preferences[BILLING_CITY_KEY] = city
            preferences[BILLING_POSTAL_CODE_KEY] = postalCode
        }
        cachedBillingData = BillingData(name, nif, license, street, city, postalCode)
    }

    // Método para obtener datos de facturación con caché
    fun getBillingData(): Flow<BillingData> {
        return dataStore.data.map { preferences ->
            cachedBillingData ?: BillingData(
                name = preferences[BILLING_NAME_KEY] ?: "",
                nif = preferences[BILLING_NIF_KEY] ?: "",
                license = preferences[BILLING_LICENSE_KEY] ?: "",
                street = preferences[BILLING_STREET_KEY] ?: "",
                city = preferences[BILLING_CITY_KEY] ?: "",
                postalCode = preferences[BILLING_POSTAL_CODE_KEY] ?: ""
            ).also {
                cachedBillingData = it
            }
        }
    }
    
    // Método para limpiar la caché
    fun clearCache() {
        cachedFirstDayOfWeek = null
        cachedSummaryIncomeType = null
        cachedSelectedDate = null
        cachedBillingData = null
    }
}

data class BillingData(
    val name: String,
    val nif: String,
    val license: String,
    val street: String,
    val city: String,
    val postalCode: String
)
