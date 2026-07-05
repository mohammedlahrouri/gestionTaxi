package com.moham.taxi.data.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.model.PlatformPaymentMethodCrossRef
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.PaymentMethodRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Servicio para crear y restaurar copias de seguridad de los datos de la aplicación
 */
class BackupService(
    private val context: Context,
    private val taxiRideRepository: TaxiRideRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val database: AppDatabase
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    companion object {
        private const val BUFFER_SIZE = 8192 // 8KB buffer
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 100L
        private const val MAX_RETRY_DELAY = 1000L
        private const val RETRY_DELAY_FACTOR = 2.0
    }

    // Mecanismo de retry para operaciones críticas
    private suspend fun <T> retryIO(
        times: Int = MAX_RETRY_ATTEMPTS,
        initialDelay: Long = INITIAL_RETRY_DELAY,
        maxDelay: Long = MAX_RETRY_DELAY,
        factor: Double = RETRY_DELAY_FACTOR,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                android.util.Log.e("BackupService", "Intento ${it + 1} fallido: ${e.message}")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block()
    }

    /**
     * Crea una copia de seguridad de los datos del mes especificado
     * @param selectedDate Una fecha dentro del mes que se desea respaldar
     * @return Uri del archivo de backup o null si ocurrió un error
     */
    suspend fun createMonthlyBackup(selectedDate: Date): Uri? = withContext(Dispatchers.IO) {
        try {
            // Obtener el rango del mes
            val (startOfMonth, endOfMonth) = com.moham.taxi.utils.DateUtils.getMonthRange(selectedDate)
            
            // Obtener todos los datos del mes
            val rides = taxiRideRepository.getTaxiRidesByDateRange(startOfMonth, endOfMonth).first()
            val expenses = expenseRepository.getExpensesByDateRange(startOfMonth, endOfMonth).first()
            val paymentMethods = paymentMethodRepository.allPaymentMethods.first()
            val servicePlatforms = database.servicePlatformDao().getAllServicePlatforms().first()
            
            // Crear el JSON de backup
            val backupJson = JSONObject()
            backupJson.put("backup_date", Date().time)
            backupJson.put("backup_version", 1)
            backupJson.put("backup_type", "monthly")
            backupJson.put("month_start", startOfMonth.time)
            backupJson.put("month_end", endOfMonth.time)
            backupJson.put("month_name", monthYearFormat.format(selectedDate))
            
            // Añadir carreras (taxi rides)
            val ridesArray = JSONArray()
            rides.forEach { ride ->
                val rideJson = JSONObject()
                rideJson.put("id", ride.id)
                rideJson.put("price", ride.price)
                ride.tip?.let { rideJson.put("tip", it) }
                rideJson.put("date", ride.date.time)
                rideJson.put("real_date", ride.realDate.time)
                rideJson.put("payment_method", ride.paymentMethod)
                ride.netPrice?.let { rideJson.put("net_price", it) }
                ride.commissionPercentAtTime?.let { rideJson.put("commission_percent_at_time", it) }
                ride.commissionVatAtTime?.let { rideJson.put("commission_vat_at_time", it) }
                if (ride.hasOwnProperty("origin")) rideJson.put("origin", ride.origin)
                if (ride.hasOwnProperty("destination")) rideJson.put("destination", ride.destination)
                ride.servicePlatform?.let { rideJson.put("service_platform", it) }
                ride.serviceType?.let { rideJson.put("service_type", it) }
                rideJson.put("ride_time", ride.rideTime)
                ride.ticketPhotoPath?.let { rideJson.put("ticket_photo_path", it) }
                ridesArray.put(rideJson)
            }
            backupJson.put("rides", ridesArray)
            
            // Añadir gastos (expenses)
            val expensesArray = JSONArray()
            expenses.forEach { expense ->
                val expenseJson = JSONObject()
                expenseJson.put("id", expense.id)
                expenseJson.put("amount", expense.amount)
                expenseJson.put("date", expense.date.time)
                expenseJson.put("real_date", expense.realDate.time)
                expenseJson.put("type", expense.type.toString())
                expenseJson.put("description", expense.description ?: "")
                expense.maintenanceKilometers?.let { expenseJson.put("maintenance_kilometers", it) }
                expense.maintenanceDetails?.let { expenseJson.put("maintenance_details", it) }
                expense.ticketPhotoPath?.let { expenseJson.put("ticket_photo_path", it) }
                expensesArray.put(expenseJson)
            }
            backupJson.put("expenses", expensesArray)
            
            val settingsJson = buildSettingsJson()
            
            // Guardar el archivo de backup en formato blocal
            val fileName = "Taxi_Backup_Mes_${monthYearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.blocal"
            return@withContext saveZipBackupToFile(fileName, backupJson.toString(4), settingsJson.toString(4))
            
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al crear backup mensual: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Crea una copia de seguridad de todos los datos de la aplicación
     * @return Uri del archivo de backup o null si ocurrió un error
     */
    suspend fun createFullBackup(): Uri? = withContext(Dispatchers.IO) {
        try {
            // Obtener todos los datos de la aplicación
            val rides = taxiRideRepository.allTaxiRides.first()
            val expenses = expenseRepository.allExpenses.first()
            val paymentMethods = paymentMethodRepository.allPaymentMethods.first()
            val servicePlatforms = database.servicePlatformDao().getAllServicePlatforms().first()
            
            // Crear el JSON de backup
            val backupJson = JSONObject()
            backupJson.put("backup_date", Date().time)
            backupJson.put("backup_version", 1)
            backupJson.put("backup_type", "full")
            
            // Añadir carreras (taxi rides)
            val ridesArray = JSONArray()
            rides.forEach { ride ->
                val rideJson = JSONObject()
                rideJson.put("id", ride.id)
                rideJson.put("price", ride.price)
                ride.tip?.let { rideJson.put("tip", it) }
                rideJson.put("date", ride.date.time)
                rideJson.put("real_date", ride.realDate.time)
                rideJson.put("payment_method", ride.paymentMethod)
                ride.netPrice?.let { rideJson.put("net_price", it) }
                ride.commissionPercentAtTime?.let { rideJson.put("commission_percent_at_time", it) }
                ride.commissionVatAtTime?.let { rideJson.put("commission_vat_at_time", it) }
                if (ride.hasOwnProperty("origin")) rideJson.put("origin", ride.origin)
                if (ride.hasOwnProperty("destination")) rideJson.put("destination", ride.destination)
                ride.servicePlatform?.let { rideJson.put("service_platform", it) }
                ride.serviceType?.let { rideJson.put("service_type", it) }
                rideJson.put("ride_time", ride.rideTime)
                ride.ticketPhotoPath?.let { rideJson.put("ticket_photo_path", it) }
                ridesArray.put(rideJson)
            }
            backupJson.put("rides", ridesArray)
            
            // Añadir gastos (expenses)
            val expensesArray = JSONArray()
            expenses.forEach { expense ->
                val expenseJson = JSONObject()
                expenseJson.put("id", expense.id)
                expenseJson.put("amount", expense.amount)
                expenseJson.put("date", expense.date.time)
                expenseJson.put("real_date", expense.realDate.time)
                expenseJson.put("type", expense.type.toString())
                expenseJson.put("description", expense.description ?: "")
                expense.maintenanceKilometers?.let { expenseJson.put("maintenance_kilometers", it) }
                expense.maintenanceDetails?.let { expenseJson.put("maintenance_details", it) }
                expense.ticketPhotoPath?.let { expenseJson.put("ticket_photo_path", it) }
                expensesArray.put(expenseJson)
            }
            backupJson.put("expenses", expensesArray)
            
            // Añadir métodos de pago
            val paymentMethodsArray = JSONArray()
            for (method in paymentMethods) {
                val methodJson = JSONObject()
                methodJson.put("id", method.id)
                methodJson.put("name", method.name)
                paymentMethodsArray.put(methodJson)
            }
            backupJson.put("payment_methods", paymentMethodsArray)

            val platformsArray = JSONArray()
            servicePlatforms.forEach { platform ->
                val platformJson = JSONObject()
                platformJson.put("id", platform.id)
                platformJson.put("name", platform.name)
                platform.commissionPercentage?.let { platformJson.put("commission_percentage", it) }
                platform.commissionVat?.let { platformJson.put("commission_vat", it) }
                platformsArray.put(platformJson)
            }
            backupJson.put("service_platforms", platformsArray)
            
            val settingsJson = buildSettingsJson()

            // Guardar el archivo de backup en formato blocal
            val fileName = "Taxi_Backup_Completo_${fileNameDateFormat.format(Date())}.blocal"
            return@withContext saveZipBackupToFile(fileName, backupJson.toString(4), settingsJson.toString(4))
            
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al crear backup completo: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Restaura los datos desde un archivo de backup
     * @param uri Uri del archivo de backup a restaurar
     * @return true si la restauración fue exitosa, false en caso contrario
     */
    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            var isZip = false
            var dataContent: String? = null
            var settingsContent: String? = null

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.util.zip.ZipInputStream(inputStream).use { zipStream ->
                        var entry = zipStream.nextEntry
                        if (entry != null) {
                            isZip = true
                            while (entry != null) {
                                val baos = java.io.ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                var count: Int
                                while (zipStream.read(buffer).also { count = it } != -1) {
                                    baos.write(buffer, 0, count)
                                }
                                val text = baos.toString(Charsets.UTF_8.name())
                                if (entry.name == "data.json") {
                                    dataContent = text
                                } else if (entry.name == "settings.json") {
                                    settingsContent = text
                                } else if (entry.name.startsWith("tickets/")) {
                                    // Restaurar foto de ticket
                                    val outFile = File(context.filesDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        fos.write(baos.toByteArray())
                                    }
                                }
                                zipStream.closeEntry()
                                entry = zipStream.nextEntry
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isZip = false
            }

            if (isZip && dataContent != null) {
                val dataJson = JSONObject(dataContent)
                val settingsJson = settingsContent?.let { JSONObject(it) }
                val backupType = dataJson.optString("backup_type", "monthly")
                
                val restoredData = when (backupType) {
                    "monthly" -> retryIO { restoreMonthlyBackup(dataJson) }
                    "full" -> retryIO { restoreFullBackup(dataJson) }
                    else -> false
                }
                
                if (restoredData && settingsJson != null) {
                    restoreSettingsFromBackup(settingsJson)
                }
                
                if (restoredData) {
                    val app = context.applicationContext as com.moham.taxi.GestionTaxiApplication
                    app.setHasCompletedOnboarding(true)
                    app.setFirstRunCompleted()
                }
                return@withContext restoredData
            } else {
                val content = retryIO {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: return@withContext false
                
                val backupJson = JSONObject(content)
                
                val backupVersion = backupJson.optInt("backup_version", 1)
                if (backupVersion > 1) {
                    android.util.Log.e("BackupService", "Versión de backup no soportada: $backupVersion")
                    return@withContext false
                }
                
                val backupType = backupJson.optString("backup_type", "monthly")

                val restored = when (backupType) {
                    "monthly" -> retryIO { restoreMonthlyBackup(backupJson) }
                    "full" -> retryIO { restoreFullBackup(backupJson) }
                    else -> {
                        android.util.Log.e("BackupService", "Tipo de backup desconocido: $backupType")
                        false
                    }
                }
                if (restored) {
                    val app = context.applicationContext as com.moham.taxi.GestionTaxiApplication
                    app.setHasCompletedOnboarding(true)
                    app.setFirstRunCompleted()
                }
                return@withContext restored
            }
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al restaurar backup: ${e.message}", e)
            false
        }
    }
    
    /**
     * Restaura un backup mensual
     */
    private suspend fun restoreMonthlyBackup(backupJson: JSONObject): Boolean {
        return try {
            val startOfMonth = Date(backupJson.getLong("month_start"))
            val endOfMonth = Date(backupJson.getLong("month_end"))
            
            withContext(Dispatchers.IO) {
                retryIO {
                    // Eliminar registros existentes en el rango de fechas
                    taxiRideRepository.deleteTaxiRidesByDateRange(startOfMonth, endOfMonth)
                    expenseRepository.deleteExpensesByDateRange(startOfMonth, endOfMonth)
                    
                    // Restaurar los datos desde el backup
                    restoreDataFromBackup(backupJson)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al restaurar backup mensual: ${e.message}", e)
            false
        }
    }
    
    /**
     * Restaura un backup completo de la aplicación
     */
    private suspend fun restoreFullBackup(backupJson: JSONObject): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                retryIO {
                    // Eliminar todos los registros
                    database.runInTransaction {
                        database.clearAllTables()
                    }
                    
                    // Restaurar los datos desde el backup
                    restoreDataFromBackup(backupJson)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al restaurar backup completo: ${e.message}", e)
            false
        }
    }
    
    /**
     * Restaura los datos desde un objeto JSON de backup
     */
    private suspend fun restoreDataFromBackup(backupJson: JSONObject) {
        // Restaurar carreras
        val ridesArray = backupJson.getJSONArray("rides")
        for (i in 0 until ridesArray.length()) {
            val rideJson = ridesArray.getJSONObject(i)
            
            // Crear objeto TaxiRide con los valores del backup
            val origin = if (rideJson.has("origin")) rideJson.getString("origin") else ""
            val destination = if (rideJson.has("destination")) rideJson.getString("destination") else ""
            
            val ride = TaxiRide(
                id = 0, // Siempre usar 0 para que Room asigne un nuevo ID
                origin = origin,
                destination = destination,
                price = rideJson.getDouble("price"),
                tip = rideJson.optDouble("tip", Double.NaN).takeIf { !it.isNaN() },
                netPrice = rideJson.optDouble("net_price", Double.NaN).takeIf { !it.isNaN() },
                commissionPercentAtTime = rideJson.optDouble("commission_percent_at_time", Double.NaN).takeIf { !it.isNaN() },
                commissionVatAtTime = rideJson.optDouble("commission_vat_at_time", Double.NaN).takeIf { !it.isNaN() },
                date = Date(rideJson.getLong("date")),
                paymentMethod = rideJson.getString("payment_method"),
                servicePlatform = rideJson.optString("service_platform").takeIf { it.isNotBlank() },
                serviceType = rideJson.optString("service_type").takeIf { it.isNotBlank() },
                rideTime = rideJson.optString("ride_time", "00:00"),
                ticketPhotoPath = rideJson.optString("ticket_photo_path").takeIf { it.isNotBlank() },
                realDate = if (rideJson.has("real_date")) Date(rideJson.getLong("real_date")) else Date(rideJson.getLong("date"))
            )
            taxiRideRepository.insertTaxiRide(ride)
        }
        
        // Restaurar gastos
        val expensesArray = backupJson.getJSONArray("expenses")
        for (i in 0 until expensesArray.length()) {
            val expenseJson = expensesArray.getJSONObject(i)
            val expense = Expense(
                id = 0, // Siempre usar 0 para que Room asigne un nuevo ID
                amount = expenseJson.getDouble("amount"),
                date = Date(expenseJson.getLong("date")),
                type = ExpenseType.valueOf(expenseJson.getString("type")),
                description = expenseJson.optString("description"),
                maintenanceKilometers = if (expenseJson.has("maintenance_kilometers")) expenseJson.getInt("maintenance_kilometers") else null,
                maintenanceDetails = expenseJson.optString("maintenance_details").takeIf { it.isNotBlank() },
                ticketPhotoPath = expenseJson.optString("ticket_photo_path").takeIf { it.isNotBlank() },
                realDate = if (expenseJson.has("real_date")) Date(expenseJson.getLong("real_date")) else Date(expenseJson.getLong("date"))
            )
            expenseRepository.insertExpense(expense)
        }
        
        // Restaurar métodos de pago si no existen
        val paymentMethodsArray = backupJson.optJSONArray("payment_methods")
        if (paymentMethodsArray != null) {
            val existingMethods = paymentMethodRepository.allPaymentMethods.first()
            val existingMethodNames = existingMethods.map { it.name }
            
            for (i in 0 until paymentMethodsArray.length()) {
                val methodJson = paymentMethodsArray.getJSONObject(i)
                val methodName = methodJson.getString("name")
                
                // Solo insertar si no existe ya
                if (!existingMethodNames.contains(methodName)) {
                    val method = PaymentMethod(
                        id = 0, // Siempre usar 0 para que Room asigne un nuevo ID
                        name = methodName
                    )
                    paymentMethodRepository.insert(method)
                }
            }
        }
    }
    
    /**
     * Comprueba si una propiedad existe en un objeto JSONObject
     */
    private fun JSONObject.hasOwnProperty(prop: String): Boolean {
        return this.has(prop) && !this.isNull(prop)
    }
    
    /**
     * Comprueba si una propiedad existe en un objeto TaxiRide
     */
    private fun TaxiRide.hasOwnProperty(prop: String): Boolean {
        return when (prop) {
            "origin" -> true
            "destination" -> true
            else -> false
        }
    }
    
    /**
     * Obtiene el rango del mes (desde el primer día hasta el último)
     */
    // Función eliminada - usar DateUtils.getMonthRange
    
    /**
     * Guarda el archivo de backup en el directorio de descargas
     * @return Uri del archivo guardado o null si ocurrió un error
     */
    private suspend fun saveZipBackupToFile(fileName: String, dataContent: String, settingsContent: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    
                    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            java.util.zip.ZipOutputStream(outputStream).use { zos ->
                                val dataEntry = java.util.zip.ZipEntry("data.json")
                                zos.putNextEntry(dataEntry)
                                zos.write(dataContent.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                                
                                val settingsEntry = java.util.zip.ZipEntry("settings.json")
                                zos.putNextEntry(settingsEntry)
                                zos.write(settingsContent.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                                
                                // Añadir fotos de tickets
                                val ticketsDir = File(context.filesDir, "tickets")
                                if (ticketsDir.exists() && ticketsDir.isDirectory) {
                                    ticketsDir.listFiles()?.forEach { file ->
                                        if (file.isFile) {
                                            val ticketEntry = java.util.zip.ZipEntry("tickets/${file.name}")
                                            zos.putNextEntry(ticketEntry)
                                            file.inputStream().use { input ->
                                                input.copyTo(zos)
                                            }
                                            zos.closeEntry()
                                        }
                                    }
                                }
                            }
                        }
                        uri
                    } ?: throw IllegalStateException("No se pudo crear el archivo")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                        throw IllegalStateException("No se pudo crear el directorio de descargas")
                    }
                    
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        java.util.zip.ZipOutputStream(fos).use { zos ->
                            val dataEntry = java.util.zip.ZipEntry("data.json")
                            zos.putNextEntry(dataEntry)
                            zos.write(dataContent.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                            
                            val settingsEntry = java.util.zip.ZipEntry("settings.json")
                            zos.putNextEntry(settingsEntry)
                            zos.write(settingsContent.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()

                            // Añadir fotos de tickets
                            val ticketsDir = File(context.filesDir, "tickets")
                            if (ticketsDir.exists() && ticketsDir.isDirectory) {
                                ticketsDir.listFiles()?.forEach { file ->
                                    if (file.isFile) {
                                        val ticketEntry = java.util.zip.ZipEntry("tickets/${file.name}")
                                        zos.putNextEntry(ticketEntry)
                                        file.inputStream().use { input ->
                                            input.copyTo(zos)
                                        }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                    }
                    
                    Uri.fromFile(file)
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupService", "Error al guardar backup ZIP: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun restoreSettingsFromBackup(settingsJson: JSONObject) {
        val app = context.applicationContext as com.moham.taxi.GestionTaxiApplication
        
        val settingsObj = settingsJson.optJSONObject("app_settings")
        if (settingsObj != null) {
            val language = settingsObj.optString("language", "")
            if (language.isNotBlank()) app.saveLanguage(language)
            
            if (settingsObj.has("daily_target")) {
                taxiRideRepository.setDailyTarget(settingsObj.getDouble("daily_target"))
            }
            if (settingsObj.has("online_backup_enabled")) {
                app.saveOnlineBackupEnabled(settingsObj.getBoolean("online_backup_enabled"))
            }
            if (settingsObj.has("summary_income_type")) {
                app.saveSummaryIncomeType(settingsObj.getInt("summary_income_type"))
            }
            if (settingsObj.has("selected_date")) {
                app.saveSelectedDate(Date(settingsObj.getLong("selected_date")))
            }
        }
        
        val billingObj = settingsJson.optJSONObject("billing_data")
        if (billingObj != null) {
            val name = billingObj.optString("name", "")
            val nif = billingObj.optString("nif", "")
            val license = billingObj.optString("license", "")
            val street = billingObj.optString("street", "")
            val city = billingObj.optString("city", "")
            val postalCode = billingObj.optString("postal_code", "")
            if (name.isNotBlank() || nif.isNotBlank() || license.isNotBlank() || street.isNotBlank() || city.isNotBlank() || postalCode.isNotBlank()) {
                app.saveBillingData(name, nif, license, street, city, postalCode)
            }
        }
        
        val paymentMethodsArray = settingsJson.optJSONArray("payment_methods")
        if (paymentMethodsArray != null) {
            for (i in 0 until paymentMethodsArray.length()) {
                val m = paymentMethodsArray.getJSONObject(i)
                val name = m.getString("name")
                val exists = database.paymentMethodDao().paymentMethodExists(name)
                if (!exists) {
                    database.paymentMethodDao().insert(PaymentMethod(name = name))
                }
            }
        }
        
        val servicePlatformsArray = settingsJson.optJSONArray("service_platforms")
        if (servicePlatformsArray != null) {
            for (i in 0 until servicePlatformsArray.length()) {
                val p = servicePlatformsArray.getJSONObject(i)
                val name = p.getString("name")
                val commissionPercentage = if (p.has("commission_percentage")) p.optDouble("commission_percentage", Double.NaN).takeIf { !it.isNaN() } else null
                val commissionVat = if (p.has("commission_vat")) p.optDouble("commission_vat", Double.NaN).takeIf { !it.isNaN() } else null
                val exists = database.servicePlatformDao().servicePlatformExists(name)
                if (!exists) {
                    database.servicePlatformDao().insert(ServicePlatform(id=0, name = name, commissionPercentage = commissionPercentage, commissionVat = commissionVat))
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
    }

    private suspend fun buildSettingsJson(): JSONObject {
        val app = context.applicationContext as com.moham.taxi.GestionTaxiApplication
        val paymentMethods = paymentMethodRepository.allPaymentMethods.first()
        val servicePlatforms = database.servicePlatformDao().getAllServicePlatforms().first()
        
        val language = app.getLanguage().first()
        val dailyTarget = taxiRideRepository.getDailyTarget().first()
        val billingData = app.getBillingData().first()
        val onlineBackupEnabled = app.isOnlineBackupEnabled().first()
        val summaryIncomeType = app.getSummaryIncomeType().first()
        val selectedDate = app.getSelectedDate().first()
        
        val settingsJson = JSONObject()
        settingsJson.put("backup_version", 2)
        settingsJson.put("backup_type", "blocal")
        
        val settingsObj = JSONObject()
        settingsObj.put("language", language)
        dailyTarget?.let { settingsObj.put("daily_target", it) }
        settingsObj.put("online_backup_enabled", onlineBackupEnabled)
        settingsObj.put("summary_income_type", summaryIncomeType)
        settingsObj.put("selected_date", selectedDate.time)
        settingsJson.put("app_settings", settingsObj)
        
        val billingObj = JSONObject()
        if (billingData.name.isNotBlank() || billingData.nif.isNotBlank() || billingData.license.isNotBlank() || billingData.street.isNotBlank() || billingData.city.isNotBlank() || billingData.postalCode.isNotBlank()) {
            billingObj.put("name", billingData.name)
            billingObj.put("nif", billingData.nif)
            billingObj.put("license", billingData.license)
            billingObj.put("street", billingData.street)
            billingObj.put("city", billingData.city)
            billingObj.put("postal_code", billingData.postalCode)
            settingsJson.put("billing_data", billingObj)
        }
        
        val methodsArray = JSONArray()
        for (m in paymentMethods) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            methodsArray.put(obj)
        }
        settingsJson.put("payment_methods", methodsArray)
        
        val platformsArray = JSONArray()
        for (p in servicePlatforms) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            p.commissionPercentage?.let { obj.put("commission_percentage", it) }
            p.commissionVat?.let { obj.put("commission_vat", it) }
            platformsArray.put(obj)
        }
        settingsJson.put("service_platforms", platformsArray)
        
        return settingsJson
    }
    
    /**
     * Comparte un archivo a través de las aplicaciones disponibles en el dispositivo
     * @param uri Uri del archivo a compartir
     * @param fileName Nombre del archivo para mostrar en el selector de aplicaciones
     */
    fun shareFile(uri: Uri, fileName: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Backup de Taxi")
                putExtra(Intent.EXTRA_TEXT, "Backup de datos generado por la aplicación Taxi: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Compartir backup a través de")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.e("BackupService", "Error al compartir el archivo: ${e.message}", e)
        }
    }
}
