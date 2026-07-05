package com.moham.taxi.data.online

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.PlatformPaymentMethodCrossRef
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class OnlineBackupRepository(
    private val context: Context,
    private val authManager: GoogleDriveAuthManager,
    private val taxiRideRepository: TaxiRideRepository,
    private val expenseRepository: ExpenseRepository,
    private val database: AppDatabase
) {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val restorePrefix = "restore_"

    private fun driveService(): Drive? {
        val credential = authManager.getCredential() ?: return null
        return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Gestion Taxi")
            .build()
    }

    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        authManager.isSignedIn()
    }

    suspend fun getLastBackupDate(): Date? = withContext(Dispatchers.IO) {
        getRestorePoints().firstOrNull()?.createdTime
    }

    suspend fun scheduleDeltaSync() {
        // Scheduling is handled by WorkManager via the worker; repository provides sync operation only
    }

    suspend fun performDeltaSync(lastSyncMillis: Long): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService() ?: return@withContext false
        val lastSyncDate = Date(lastSyncMillis)
        val newRides = taxiRideRepository.getTaxiRidesByDateRange(lastSyncDate, Date()).first()
        val newExpenses = expenseRepository.getExpensesByDateRange(lastSyncDate, Date()).first()

        if (newRides.isEmpty() && newExpenses.isEmpty()) return@withContext true

        val payload = buildDeltaPayload(newRides, newExpenses, lastSyncMillis)
        val compressedPayload = zipPayload(payload, "{}")
        val fileMetadata = File().apply {
            name = "${restorePrefix}${dateFormat.format(Date())}.blocal"
            parents = listOf("appDataFolder")
        }
        val content = com.google.api.client.http.ByteArrayContent("application/octet-stream", compressedPayload)
        val created = drive.files().create(fileMetadata, content)
            .setFields("id,createdTime")
            .execute()
        enforceRestorePointLimit(drive)
        created?.id != null
    }

    suspend fun createRestorePoint(): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService() ?: return@withContext false
        val hasChanges = hasChangesSinceLastRestore()
        if (!hasChanges) {
            return@withContext true
        }
        val rides = taxiRideRepository.allTaxiRides.first()
        val expenses = expenseRepository.allExpenses.first()
        val paymentMethods = database.paymentMethodDao().getAllPaymentMethods().first()
        val servicePlatforms = database.servicePlatformDao().getAllServicePlatforms().first()
        val app = context.applicationContext as GestionTaxiApplication
        val language = app.getLanguage().first()
        val dailyTarget = taxiRideRepository.getDailyTarget().first()
        val billingData = app.getBillingData().first()
        val onlineBackupEnabled = app.isOnlineBackupEnabled().first()
        val summaryIncomeType = app.getSummaryIncomeType().first()
        val selectedDate = app.getSelectedDate().first()

        val (dataPayload, settingsPayload) = buildBackupPayloads(
            rides,
            expenses,
            paymentMethods,
            servicePlatforms,
            language,
            dailyTarget,
            billingData,
            onlineBackupEnabled,
            summaryIncomeType,
            selectedDate.time
        )
        val compressedPayload = zipPayload(dataPayload, settingsPayload)
        val fileMetadata = File().apply {
            name = "${restorePrefix}${dateFormat.format(Date())}.blocal"
            parents = listOf("appDataFolder")
        }
        val content = com.google.api.client.http.ByteArrayContent("application/octet-stream", compressedPayload)
        val created = drive.files().create(fileMetadata, content)
            .setFields("id,createdTime")
            .execute()
        enforceRestorePointLimit(drive)
        created?.id != null
    }

    suspend fun getRestorePoints(): List<RestorePoint> = withContext(Dispatchers.IO) {
        val drive = driveService() ?: return@withContext emptyList()
        val files = drive.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id,name,createdTime)")
            .execute()
            .files
            ?.filter { it.name?.startsWith(restorePrefix) == true }
            ?.sortedByDescending { it.createdTime.value }
            ?: emptyList()
        files.map {
            RestorePoint(
                id = it.id,
                name = it.name ?: "",
                createdTime = Date(it.createdTime.value)
            )
        }
    }

    suspend fun restoreFromRestorePoint(restorePointId: String): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService() ?: return@withContext false
        val fileName = drive.files().get(restorePointId).setFields("name").execute()?.name
        val content = readBackupContent(drive, restorePointId, fileName)
        
        if (content.isLegacy) {
            applyRestorePayload(content.dataJson, null)
        } else {
            applyRestorePayload(content.dataJson, content.settingsJson)
        }
        
        val app = context.applicationContext as GestionTaxiApplication
        app.setHasCompletedOnboarding(true)
        app.setFirstRunCompleted()
        true
    }

    suspend fun restoreFromDrive(): Boolean = withContext(Dispatchers.IO) {
        val latest = getRestorePoints().firstOrNull() ?: return@withContext false
        restoreFromRestorePoint(latest.id)
    }

    suspend fun hasChangesSinceLastRestore(): Boolean = withContext(Dispatchers.IO) {
        val drive = driveService() ?: return@withContext true
        val latest = getRestorePoints().firstOrNull() ?: return@withContext true
        val rides = taxiRideRepository.allTaxiRides.first()
        val expenses = expenseRepository.allExpenses.first()
        val paymentMethods = database.paymentMethodDao().getAllPaymentMethods().first()
        val servicePlatforms = database.servicePlatformDao().getAllServicePlatforms().first()
        val app = context.applicationContext as GestionTaxiApplication
        val language = app.getLanguage().first()
        val dailyTarget = taxiRideRepository.getDailyTarget().first()
        val billingData = app.getBillingData().first()
        val onlineBackupEnabled = app.isOnlineBackupEnabled().first()
        val summaryIncomeType = app.getSummaryIncomeType().first()
        val selectedDate = app.getSelectedDate().first()
        val (dataPayload, settingsPayload) = buildBackupPayloads(
            rides,
            expenses,
            paymentMethods,
            servicePlatforms,
            language,
            dailyTarget,
            billingData,
            onlineBackupEnabled,
            summaryIncomeType,
            selectedDate.time
        )
        val latestContent = readBackupContent(drive, latest.id, latest.name)
        if (latestContent.isLegacy) {
            return@withContext true // Force a new backup to convert from legacy format
        }
        dataPayload != latestContent.dataJson || settingsPayload != latestContent.settingsJson
    }

    private fun buildDeltaPayload(rides: List<TaxiRide>, expenses: List<Expense>, lastSyncMillis: Long): String {
        val root = org.json.JSONObject()
        root.put("last_sync", lastSyncMillis)
        root.put("backup_type", "delta")
        val ridesArray = org.json.JSONArray()
        rides.forEach { r ->
            val obj = org.json.JSONObject()
            obj.put("id", r.id)
            obj.put("price", r.price)
            r.tip?.let { obj.put("tip", it) }
            obj.put("date", r.date.time)
            obj.put("real_date", r.realDate.time)
            obj.put("payment_method", r.paymentMethod)
            obj.put("origin", r.origin)
            obj.put("destination", r.destination)
            obj.put("service_platform", r.servicePlatform ?: "Directo")
            obj.put("service_type", r.serviceType ?: TaxiRide.SERVICE_TYPE_METER)
            obj.put("ride_time", r.rideTime)
            r.netPrice?.let { obj.put("net_price", it) }
            r.commissionPercentAtTime?.let { obj.put("commission_percent_at_time", it) }
            r.commissionVatAtTime?.let { obj.put("commission_vat_at_time", it) }
            ridesArray.put(obj)
        }
        val expensesArray = org.json.JSONArray()
        expenses.forEach { e ->
            val obj = org.json.JSONObject()
            obj.put("id", e.id)
            obj.put("amount", e.amount)
            obj.put("date", e.date.time)
            obj.put("real_date", e.realDate.time)
            obj.put("type", e.type.name)
            e.description?.let { obj.put("description", it) }
            e.maintenanceKilometers?.let { obj.put("maintenance_kilometers", it) }
            e.maintenanceDetails?.let { obj.put("maintenance_details", it) }
            expensesArray.put(obj)
        }
        root.put("rides", ridesArray)
        root.put("expenses", expensesArray)
        return root.toString()
    }

    private fun buildBackupPayloads(
        rides: List<TaxiRide>,
        expenses: List<Expense>,
        paymentMethods: List<PaymentMethod>,
        servicePlatforms: List<ServicePlatform>,
        language: String,
        dailyTarget: Double?,
        billingData: com.moham.taxi.BillingData,
        onlineBackupEnabled: Boolean,
        summaryIncomeType: Int,
        selectedDateMillis: Long
    ): Pair<String, String> {
        val backupJson = org.json.JSONObject()
        backupJson.put("backup_date", Date().time)
        backupJson.put("backup_version", 1)
        backupJson.put("backup_type", "full")

        val settingsJson = org.json.JSONObject()
        settingsJson.put("backup_version", 2)
        settingsJson.put("backup_type", "blocal")

        val settings = org.json.JSONObject()
        settings.put("language", language)
        dailyTarget?.let { settings.put("daily_target", it) }
        settings.put("online_backup_enabled", onlineBackupEnabled)
        settings.put("summary_income_type", summaryIncomeType)
        settings.put("selected_date", selectedDateMillis)
        settingsJson.put("app_settings", settings)
        
        val billing = org.json.JSONObject()
        if (billingData.name.isNotBlank() ||
            billingData.nif.isNotBlank() ||
            billingData.license.isNotBlank() ||
            billingData.street.isNotBlank() ||
            billingData.city.isNotBlank() ||
            billingData.postalCode.isNotBlank()
        ) {
            billing.put("name", billingData.name)
            billing.put("nif", billingData.nif)
            billing.put("license", billingData.license)
            billing.put("street", billingData.street)
            billing.put("city", billingData.city)
            billing.put("postal_code", billingData.postalCode)
            settingsJson.put("billing_data", billing)
        }
        val ridesArray = org.json.JSONArray()
        val sortedRides = rides.sortedWith(
            compareBy<TaxiRide>(
                { it.date.time },
                { it.price },
                { it.paymentMethod },
                { it.origin },
                { it.destination },
                { it.servicePlatform ?: "" },
                { it.serviceType ?: "" },
                { it.rideTime }
            )
        )
        sortedRides.forEach { r ->
            val obj = org.json.JSONObject()
            obj.put("id", r.id)
            obj.put("price", r.price)
            r.tip?.let { obj.put("tip", it) }
            obj.put("date", r.date.time)
            obj.put("real_date", r.realDate.time)
            obj.put("payment_method", r.paymentMethod)
            obj.put("origin", r.origin)
            obj.put("destination", r.destination)
            obj.put("service_platform", r.servicePlatform ?: "Directo")
            obj.put("service_type", r.serviceType ?: TaxiRide.SERVICE_TYPE_METER)
            obj.put("ride_time", r.rideTime)
            r.netPrice?.let { obj.put("net_price", it) }
            r.commissionPercentAtTime?.let { obj.put("commission_percent_at_time", it) }
            r.commissionVatAtTime?.let { obj.put("commission_vat_at_time", it) }
            ridesArray.put(obj)
        }
        val expensesArray = org.json.JSONArray()
        val sortedExpenses = expenses.sortedWith(
            compareBy<Expense>(
                { it.date.time },
                { it.amount },
                { it.type.name },
                { it.description ?: "" }
            )
        )
        sortedExpenses.forEach { e ->
            val obj = org.json.JSONObject()
            obj.put("id", e.id)
            obj.put("amount", e.amount)
            obj.put("date", e.date.time)
            obj.put("real_date", e.realDate.time)
            obj.put("type", e.type.name)
            e.description?.let { obj.put("description", it) }
            e.maintenanceKilometers?.let { obj.put("maintenance_kilometers", it) }
            e.maintenanceDetails?.let { obj.put("maintenance_details", it) }
            expensesArray.put(obj)
        }
        val methodsArray = org.json.JSONArray()
        val sortedMethods = paymentMethods.sortedBy { it.name }
        sortedMethods.forEach { m ->
            val obj = org.json.JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            methodsArray.put(obj)
        }
        val platformsArray = org.json.JSONArray()
        val sortedPlatforms = servicePlatforms.sortedBy { it.name }
        sortedPlatforms.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            p.commissionPercentage?.let { obj.put("commission_percentage", it) }
            p.commissionVat?.let { obj.put("commission_vat", it) }
            platformsArray.put(obj)
        }
        backupJson.put("rides", ridesArray)
        backupJson.put("expenses", expensesArray)
        backupJson.put("payment_methods", methodsArray)
        backupJson.put("service_platforms", platformsArray)
        
        settingsJson.put("payment_methods", methodsArray)
        settingsJson.put("service_platforms", platformsArray)
        
        return Pair(backupJson.toString(4), settingsJson.toString(4))
    }

    private suspend fun applyRestorePayload(dataJsonString: String, settingsJsonString: String?) {
        val dataObj = org.json.JSONObject(dataJsonString)
        val settingsObjWrapper = settingsJsonString?.let { org.json.JSONObject(it) }
        
        val rides = dataObj.optJSONArray("rides") ?: org.json.JSONArray()
        val expenses = dataObj.optJSONArray("expenses") ?: org.json.JSONArray()
        
        val app = context.applicationContext as GestionTaxiApplication
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.clearAllTables()
                
                // Los metodos de pago y plataformas pueden venir en dataObj (legacy) o settingsObj (nuevo formato)
                val paymentMethods = settingsObjWrapper?.optJSONArray("payment_methods") ?: dataObj.optJSONArray("payment_methods")
                if (paymentMethods != null) {
                    for (i in 0 until paymentMethods.length()) {
                        val m = paymentMethods.getJSONObject(i)
                        val name = m.getString("name")
                        val exists = database.paymentMethodDao().paymentMethodExists(name)
                        if (!exists) {
                            database.paymentMethodDao().insert(PaymentMethod(name = name))
                        }
                    }
                }
                val servicePlatforms = settingsObjWrapper?.optJSONArray("service_platforms") ?: dataObj.optJSONArray("service_platforms")
                if (servicePlatforms != null) {
                    for (i in 0 until servicePlatforms.length()) {
                        val p = servicePlatforms.getJSONObject(i)
                        val name = p.getString("name")
                        val commissionPercentage = if (p.has("commission_percentage")) p.optDouble("commission_percentage", Double.NaN).takeIf { !it.isNaN() } else null
                        val commissionVat = if (p.has("commission_vat")) p.optDouble("commission_vat", Double.NaN).takeIf { !it.isNaN() } else null
                        val exists = database.servicePlatformDao().servicePlatformExists(name)
                        if (!exists) {
                            database.servicePlatformDao().insert(ServicePlatform(name = name, commissionPercentage = commissionPercentage, commissionVat = commissionVat))
                        } else if (commissionPercentage != null || commissionVat != null) {
                            val existing = database.servicePlatformDao().getServicePlatformByName(name)
                            if (existing != null) {
                                val updated = existing.copy(
                                    commissionPercentage = commissionPercentage ?: existing.commissionPercentage,
                                    commissionVat = commissionVat ?: existing.commissionVat
                                )
                                database.servicePlatformDao().update(updated)
                            }
                        }
                    }
                }

                val methods = database.paymentMethodDao().getAllPaymentMethods().first()
                val platforms = database.servicePlatformDao().getAllServicePlatforms().first()
                database.platformPaymentMethodDao().insertAll(
                    platforms.flatMap { platform ->
                        methods.map { method ->
                            PlatformPaymentMethodCrossRef(platformId = platform.id, paymentMethodId = method.id)
                        }
                    }
                )
                for (i in 0 until rides.length()) {
                    val r = rides.getJSONObject(i)
                    val origin = r.optString("origin")
                    val destination = r.optString("destination")
                    val price = r.getDouble("price")
                    val tip = r.optDouble("tip", Double.NaN).takeIf { !it.isNaN() }
                    val paymentMethod = r.getString("payment_method")
                    val date = Date(r.getLong("date"))
                    val realDate = if (r.has("real_date")) Date(r.getLong("real_date")) else date
                    val serviceType = r.optString("service_type", TaxiRide.SERVICE_TYPE_METER)
                    val servicePlatform = r.optString("service_platform", "Directo")
                    val rideTime = r.optString("ride_time", "00:00")
                    val exists = database.taxiRideDao().taxiRideExists(
                        date = date,
                        price = price,
                        tip = tip,
                        paymentMethod = paymentMethod,
                        origin = origin,
                        destination = destination,
                        serviceType = serviceType,
                        servicePlatform = servicePlatform,
                        rideTime = rideTime
                    )
                    if (!exists) {
                        val ride = TaxiRide(
                            id = 0,
                            origin = origin,
                            destination = destination,
                            price = price,
                            tip = tip,
                            paymentMethod = paymentMethod,
                            date = date,
                            serviceType = serviceType,
                            servicePlatform = servicePlatform,
                            rideTime = rideTime,
                            netPrice = r.optDouble("net_price", Double.NaN).takeIf { !it.isNaN() },
                            commissionPercentAtTime = r.optDouble("commission_percent_at_time", Double.NaN).takeIf { !it.isNaN() },
                            commissionVatAtTime = r.optDouble("commission_vat_at_time", Double.NaN).takeIf { !it.isNaN() },
                            realDate = realDate
                        )
                        database.taxiRideDao().insert(ride)
                    }
                }
                for (i in 0 until expenses.length()) {
                    val e = expenses.getJSONObject(i)
                    val amount = e.getDouble("amount")
                    val date = Date(e.getLong("date"))
                    val realDate = if (e.has("real_date")) Date(e.getLong("real_date")) else date
                    val type = com.moham.taxi.data.model.ExpenseType.valueOf(e.getString("type"))
                    val description = e.optString("description", "").ifBlank { null }
                    val maintenanceKilometers = if (e.has("maintenance_kilometers") && !e.isNull("maintenance_kilometers")) {
                        e.getInt("maintenance_kilometers")
                    } else {
                        null
                    }
                    val maintenanceDetails = e.optString("maintenance_details", "").ifBlank { null }
                    val exists = database.expenseDao().expenseExists(
                        date = date,
                        amount = amount,
                        type = type,
                        description = description
                    )
                    if (!exists) {
                        val expense = Expense(
                            id = 0,
                            type = type,
                            description = description,
                            maintenanceKilometers = maintenanceKilometers,
                            maintenanceDetails = maintenanceDetails,
                            amount = amount,
                            date = date,
                            realDate = realDate
                        )
                        database.expenseDao().insert(expense)
                    }
                }
            }
            val settings = settingsObjWrapper?.optJSONObject("app_settings") ?: dataObj.optJSONObject("settings")
            if (settings != null) {
                val language = settings.optString("language", "")
                if (language.isNotBlank()) {
                    app.saveLanguage(language)
                }
                if (settings.has("daily_target")) {
                    val dt = settings.optDouble("daily_target", Double.NaN)
                    if (!dt.isNaN()) {
                        taxiRideRepository.setDailyTarget(dt)
                    }
                }
                if (settings.has("online_backup_enabled")) {
                    val enabled = settings.optBoolean("online_backup_enabled", false)
                    app.saveOnlineBackupEnabled(enabled)
                }
                if (settings.has("summary_income_type")) {
                    val summaryType = settings.optInt("summary_income_type", 0)
                    app.saveSummaryIncomeType(summaryType)
                }
                if (settings.has("selected_date")) {
                    val timestamp = settings.optLong("selected_date", 0L)
                    if (timestamp > 0L) {
                        app.saveSelectedDate(Date(timestamp))
                    }
                }
            }
            val billing = settingsObjWrapper?.optJSONObject("billing_data") ?: dataObj.optJSONObject("billing_data")
            if (billing != null) {
                val name = billing.optString("name", "")
                val nif = billing.optString("nif", "")
                val license = billing.optString("license", "")
                val street = billing.optString("street", "")
                val city = billing.optString("city", "")
                val postalCode = billing.optString("postal_code", "")
                if (name.isNotBlank() || nif.isNotBlank() || license.isNotBlank() ||
                    street.isNotBlank() || city.isNotBlank() || postalCode.isNotBlank()
                ) {
                    app.saveBillingData(name, nif, license, street, city, postalCode)
                }
            }
        }
    }

    private fun zipPayload(dataContent: String, settingsContent: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zos ->
            zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            
            zos.putNextEntry(java.util.zip.ZipEntry("data.json"))
            zos.write(dataContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            zos.putNextEntry(java.util.zip.ZipEntry("settings.json"))
            zos.write(settingsContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return outputStream.toByteArray()
    }

    data class BackupContent(val dataJson: String, val settingsJson: String?, val isLegacy: Boolean)

    private fun readBackupContent(drive: Drive, fileId: String, fileName: String?): BackupContent {
        val stream = drive.files().get(fileId).executeMediaAsInputStream()
        if (isGzipFileName(fileName)) {
            val content = GZIPInputStream(stream).bufferedReader().use { it.readText() }
            return BackupContent(content, null, true)
        } else {
            // Manejar como ZIP (.blocal)
            var dataContent = ""
            var settingsContent = ""
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        baos.write(buffer, 0, count)
                    }
                    val text = baos.toString(Charsets.UTF_8.name())
                    if (entry.name == "data.json") {
                        dataContent = text
                    } else if (entry.name == "settings.json") {
                        settingsContent = text
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            // Si dataContent esta vacio, tal vez era un archivo simple (fallback)
            if (dataContent.isEmpty()) {
                val fallbackStream = drive.files().get(fileId).executeMediaAsInputStream()
                val content = fallbackStream.bufferedReader().use { it.readText() }
                return BackupContent(content, null, true)
            }
            return BackupContent(dataContent, settingsContent, false)
        }
    }

    private fun isGzipFileName(fileName: String?): Boolean {
        return fileName?.lowercase(Locale.getDefault())?.endsWith(".gz") == true
    }

    private fun enforceRestorePointLimit(drive: Drive) {
        val snapshots = drive.files().list()
            .setSpaces("appDataFolder")
            .setFields("files(id,name,createdTime)")
            .execute()
            .files
            ?.filter { it.name?.startsWith(restorePrefix) == true }
            ?.sortedBy { it.createdTime.value }
            ?: emptyList()
        if (snapshots.size > 3) {
            val toDelete = snapshots.take(snapshots.size - 3)
            toDelete.forEach { drive.files().delete(it.id).execute() }
        }
    }
}

data class RestorePoint(
    val id: String,
    val name: String,
    val createdTime: Date
)
