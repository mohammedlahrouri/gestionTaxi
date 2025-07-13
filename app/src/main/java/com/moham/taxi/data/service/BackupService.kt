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
    private val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
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
                rideJson.put("date", ride.date.time)
                rideJson.put("payment_method", ride.paymentMethod)
                if (ride.hasOwnProperty("origin")) rideJson.put("origin", ride.origin)
                if (ride.hasOwnProperty("destination")) rideJson.put("destination", ride.destination)
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
                expenseJson.put("type", expense.type.toString())
                expenseJson.put("description", expense.description ?: "")
                expensesArray.put(expenseJson)
            }
            backupJson.put("expenses", expensesArray)
            
            // Añadir métodos de pago (pueden ser necesarios para la restauración)
            val paymentMethodsArray = JSONArray()
            for (method in paymentMethods) {
                val methodJson = JSONObject()
                methodJson.put("id", method.id)
                methodJson.put("name", method.name)
                paymentMethodsArray.put(methodJson)
            }
            backupJson.put("payment_methods", paymentMethodsArray)
            
            // Guardar el archivo de backup
            val fileName = "Taxi_Backup_Mes_${monthYearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.json"
            return@withContext saveBackupToFile(fileName, backupJson.toString(4))
            
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
                rideJson.put("date", ride.date.time)
                rideJson.put("payment_method", ride.paymentMethod)
                if (ride.hasOwnProperty("origin")) rideJson.put("origin", ride.origin)
                if (ride.hasOwnProperty("destination")) rideJson.put("destination", ride.destination)
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
                expenseJson.put("type", expense.type.toString())
                expenseJson.put("description", expense.description ?: "")
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
            
            // Guardar el archivo de backup
            val fileName = "Taxi_Backup_Completo_${fileNameDateFormat.format(Date())}.json"
            return@withContext saveBackupToFile(fileName, backupJson.toString(4))
            
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
            
            return@withContext when (backupType) {
                "monthly" -> retryIO { restoreMonthlyBackup(backupJson) }
                "full" -> retryIO { restoreFullBackup(backupJson) }
                else -> {
                    android.util.Log.e("BackupService", "Tipo de backup desconocido: $backupType")
                    false
                }
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
                date = Date(rideJson.getLong("date")),
                paymentMethod = rideJson.getString("payment_method")
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
                description = expenseJson.optString("description")
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
    private suspend fun saveBackupToFile(fileName: String, content: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    
                    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            OutputStreamWriter(outputStream, Charsets.UTF_8).buffered(BUFFER_SIZE).use { writer ->
                                writer.write(content)
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
                    FileOutputStream(file).buffered(BUFFER_SIZE).use { fos ->
                        OutputStreamWriter(fos, Charsets.UTF_8).buffered(BUFFER_SIZE).use { writer ->
                            writer.write(content)
                        }
                    }
                    
                    Uri.fromFile(file)
                }
            } catch (e: Exception) {
                android.util.Log.e("BackupService", "Error al guardar backup: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Comparte un archivo a través de las aplicaciones disponibles en el dispositivo
     * @param uri Uri del archivo a compartir
     * @param fileName Nombre del archivo para mostrar en el selector de aplicaciones
     */
    fun shareFile(uri: Uri, fileName: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
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
