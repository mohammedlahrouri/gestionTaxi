package com.moham.taxi

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.os.LocaleListCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Locale
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.online.GoogleDriveAuthManager
import com.moham.taxi.data.online.OnlineBackupRepository
import com.moham.taxi.data.online.OnlineBackupWorker
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.PaymentMethodRepository
import com.moham.taxi.data.repository.ServicePlatformRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import com.moham.taxi.data.service.ExportService
import com.moham.taxi.data.service.BackupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

// Extension property para DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taxi_settings")

class GestionTaxiApplication : Application() {
    
    // Scope único optimizado para todas las operaciones
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Sistema de caché para preferencias frecuentemente usadas
    private var cachedSummaryIncomeType: Int? = null
    private var cachedSelectedDate: Date? = null
    var cachedBillingData: BillingData? = null
    var currentQuote: com.moham.taxi.data.model.QuoteData? = null
    
    // Base de datos y DAOs
    private val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    private val taxiRideDao by lazy { database.taxiRideDao() }
    private val expenseDao by lazy { database.expenseDao() }
    private val paymentMethodDao by lazy { database.paymentMethodDao() }
    private val servicePlatformDao by lazy { database.servicePlatformDao() }
    
    // Repositorios
    val taxiRideRepository by lazy { TaxiRideRepository(taxiRideDao, database, this) }
    val expenseRepository by lazy { ExpenseRepository(expenseDao, database, this) }
    val paymentMethodRepository by lazy { PaymentMethodRepository(paymentMethodDao, database) }
    val servicePlatformRepository by lazy { ServicePlatformRepository(servicePlatformDao) }
    val tariffRepository by lazy { com.moham.taxi.data.repository.TariffRepository(database.tariffDao()) }
    val surchargeRepository by lazy { com.moham.taxi.data.repository.SurchargeRepository(database.surchargeDao()) }
    
    // Servicio de exportación
    val exportService by lazy { ExportService(this, taxiRideRepository, expenseRepository) }
    
    // Servicio de backup
    val backupService by lazy { BackupService(this, taxiRideRepository, expenseRepository, paymentMethodRepository, database) }

    val googleDriveAuthManager by lazy { GoogleDriveAuthManager(this) }
    val onlineBackupRepository by lazy {
        OnlineBackupRepository(this, googleDriveAuthManager, taxiRideRepository, expenseRepository, database)
    }
    
    // Claves para almacenar preferencias
    companion object {
        val SELECTED_DATE_KEY = longPreferencesKey("selected_date")
        val DAILY_TARGET_KEY = stringPreferencesKey("daily_target")
        val SUMMARY_INCOME_TYPE_KEY = intPreferencesKey("summary_income_type")
        val BILLING_NAME_KEY = stringPreferencesKey("billing_name")
        val BILLING_NIF_KEY = stringPreferencesKey("billing_nif")
        val BILLING_LICENSE_KEY = stringPreferencesKey("billing_license")
        val BILLING_STREET_KEY = stringPreferencesKey("billing_street")
        val BILLING_CITY_KEY = stringPreferencesKey("billing_city")
        val BILLING_POSTAL_CODE_KEY = stringPreferencesKey("billing_postal_code")
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        val ONLINE_BACKUP_ENABLED_KEY = booleanPreferencesKey("online_backup_enabled")
        val ONLINE_BACKUP_LAST_SYNC_KEY = longPreferencesKey("online_backup_last_sync")
        val PENDING_BACKUP_KEY = booleanPreferencesKey("pending_backup")
        val UPDATE_V3_DIALOG_SHOWN_KEY = booleanPreferencesKey("update_v3_dialog_shown")
        val LAST_INVOICE_NUMBER_KEY = intPreferencesKey("last_invoice_number")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
        val DAILY_CHALLENGE_ENABLED_KEY = booleanPreferencesKey("daily_challenge_enabled")
        val SHOW_RIDE_ORIGIN_DESTINATION_KEY = booleanPreferencesKey("show_ride_origin_destination")
        val TIPS_ENABLED_KEY = booleanPreferencesKey("tips_enabled")
        val IS_FIRST_RUN_KEY = booleanPreferencesKey("is_first_run")
        val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        val LAST_SEEN_VERSION_CODE_KEY = longPreferencesKey("last_seen_version_code")
        val STORED_FIRST_INSTALL_TIME_KEY = longPreferencesKey("stored_first_install_time")
        val TICKET_PHOTOS_ENABLED_KEY = booleanPreferencesKey("ticket_photos_enabled")
    }
    
    // Método para guardar si el reto diario está habilitado
    suspend fun saveDailyChallengeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DAILY_CHALLENGE_ENABLED_KEY] = enabled
        }
    }

    // Método para obtener si el reto diario está habilitado
    fun isDailyChallengeEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[DAILY_CHALLENGE_ENABLED_KEY] ?: false
        }
    }

    suspend fun saveRideOriginDestinationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_RIDE_ORIGIN_DESTINATION_KEY] = enabled
        }
    }

    fun isRideOriginDestinationEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[SHOW_RIDE_ORIGIN_DESTINATION_KEY] ?: true
        }
    }

    suspend fun saveTipsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIPS_ENABLED_KEY] = enabled
        }
    }

    fun isTipsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[TIPS_ENABLED_KEY] ?: false
        }
    }

    suspend fun saveTicketPhotosEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TICKET_PHOTOS_ENABLED_KEY] = enabled
        }
    }

    fun isTicketPhotosEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[TICKET_PHOTOS_ENABLED_KEY] ?: false
        }
    }

    fun isFirstRun(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[IS_FIRST_RUN_KEY] ?: true
        }
    }

    suspend fun setFirstRunCompleted() {
        dataStore.edit { preferences ->
            preferences[IS_FIRST_RUN_KEY] = false
        }
    }

    fun hasCompletedOnboarding(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING_KEY] ?: false
        }
    }

    suspend fun getHasCompletedOnboardingOrNull(): Boolean? {
        val prefs = dataStore.data.first()
        return if (prefs.contains(HAS_COMPLETED_ONBOARDING_KEY)) prefs[HAS_COMPLETED_ONBOARDING_KEY] else null
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING_KEY] = completed
        }
    }

    suspend fun getLastSeenVersionCodeOrNull(): Long? {
        val prefs = dataStore.data.first()
        return if (prefs.contains(LAST_SEEN_VERSION_CODE_KEY)) prefs[LAST_SEEN_VERSION_CODE_KEY] else null
    }

    suspend fun setLastSeenVersionCode(versionCode: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SEEN_VERSION_CODE_KEY] = versionCode
        }
    }

    suspend fun getStoredFirstInstallTimeOrNull(): Long? {
        val prefs = dataStore.data.first()
        return if (prefs.contains(STORED_FIRST_INSTALL_TIME_KEY)) prefs[STORED_FIRST_INSTALL_TIME_KEY] else null
    }

    suspend fun setStoredFirstInstallTime(value: Long) {
        dataStore.edit { preferences ->
            preferences[STORED_FIRST_INSTALL_TIME_KEY] = value
        }
    }

    suspend fun setIsFirstRun(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_FIRST_RUN_KEY] = value
        }
    }
    
    // Método para guardar el tema de la aplicación
    suspend fun saveAppTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme
        }
    }

    // Método para obtener el tema de la aplicación
    fun getAppTheme(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[APP_THEME_KEY] ?: "blue" // "blue" or "green"
        }
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
    
    // Método para obtener el primer día de la semana (siempre lunes por defecto)
    fun getFirstDayOfWeek(): Flow<Int> {
        return dataStore.data.map { preferences ->
            2
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

    suspend fun saveLastInvoiceNumber(number: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_INVOICE_NUMBER_KEY] = number
        }
    }

    fun getLastInvoiceNumber(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[LAST_INVOICE_NUMBER_KEY] ?: 0
        }
    }
    
    // Método para limpiar la caché
    fun clearCache() {
        cachedSummaryIncomeType = null
        cachedSelectedDate = null
        cachedBillingData = null
    }
    
    // Método para guardar el idioma seleccionado
    suspend fun saveLanguage(languageCode: String) {
        val normalized = languageCode
            .lowercase(Locale.getDefault())
            .trim()
            .substringBefore("-")
            .substringBefore("_")
        val localeList = when (normalized) {
            "", "system" -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(normalized)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    // Método para obtener el idioma seleccionado
    fun getLanguage(): Flow<String> {
        val locales = AppCompatDelegate.getApplicationLocales()
        val language = if (locales.isEmpty) {
            detectDeviceLanguage()
        } else {
            locales[0]?.language ?: detectDeviceLanguage()
        }
        return flowOf(language)
    }

    suspend fun saveOnlineBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONLINE_BACKUP_ENABLED_KEY] = enabled
        }
    }

    fun isOnlineBackupEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[ONLINE_BACKUP_ENABLED_KEY] ?: false
        }
    }

    suspend fun saveLastOnlineBackupSync(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[ONLINE_BACKUP_LAST_SYNC_KEY] = timestamp
        }
    }

    fun getLastOnlineBackupSync(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[ONLINE_BACKUP_LAST_SYNC_KEY] ?: 0L
        }
    }
    
    suspend fun savePendingBackup(pending: Boolean) {
        dataStore.edit { preferences ->
            preferences[PENDING_BACKUP_KEY] = pending
        }
    }
    
    fun isPendingBackup(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[PENDING_BACKUP_KEY] ?: false
        }
    }

    fun hasShownUpdateV3Dialog(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[UPDATE_V3_DIALOG_SHOWN_KEY] ?: false
        }
    }

    fun getCurrentCity(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("current_city")] ?: "Madrid"
        }
    }

    suspend fun saveCurrentCity(city: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("current_city")] = city
        }
    }

    suspend fun saveHasShownUpdateV3Dialog(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[UPDATE_V3_DIALOG_SHOWN_KEY] = shown
        }
    }

    fun scheduleOnlineBackupDebounced() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OnlineBackupWorker>()
            .setInitialDelay(3, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "online_backup_debounced",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
    
    fun scheduleOnlineBackupImmediate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<OnlineBackupWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "online_backup_immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        return hasInternet && hasTransport
    }

    private suspend fun syncOnlineBackupOnStartup() {
        val enabled = isOnlineBackupEnabled().first()
        val signedIn = googleDriveAuthManager.isSignedIn()
        if (!enabled || !signedIn || !isNetworkAvailable()) return
        val ok = onlineBackupRepository.createRestorePoint()
        if (ok) {
            saveLastOnlineBackupSync(System.currentTimeMillis())
        }
    }
    
    private fun detectDeviceLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        return when (locale.language.lowercase(Locale.getDefault())) {
            "es" -> "es"
            "fr" -> "fr"
            else -> "en"
        }
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
