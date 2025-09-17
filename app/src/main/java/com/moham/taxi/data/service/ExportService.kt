package com.moham.taxi.data.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resultado de las operaciones de exportación
 */
sealed class ExportResult {
    data class Success(val uri: Uri, val fileName: String) : ExportResult()
    data class Error(val message: String, val exception: Throwable? = null) : ExportResult()
}

/**
 * Resultado de las operaciones de compartir archivos
 */
sealed class ShareResult {
    object Success : ShareResult()
    data class Error(val message: String, val exception: Throwable? = null) : ShareResult()
}

class ExportService(
    private val context: Context,
    private val taxiRideRepository: TaxiRideRepository,
    private val expenseRepository: ExpenseRepository
) {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Formatea un valor monetario para exportación
     */
    private fun formatMoney(amount: Double): String {
        // Formato para Excel: valor numérico con punto decimal y "EUR" en lugar del símbolo €
        return "\"${String.format(Locale.US, "%.2f", amount)} EUR\""
    }
    
    /**
     * Exporta los datos financieros del día seleccionado
     * @param selectedDate Fecha seleccionada para exportar datos
     * @return ExportResult con el resultado de la operación
     */
    suspend fun exportDayData(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val rides = taxiRideRepository.getRidesForDate(selectedDate).first()
            val expenses = expenseRepository.getExpensesForDate(selectedDate).first()
            val income = taxiRideRepository.getIncomeForDate(selectedDate)
            val totalExpenses = expenseRepository.getExpensesTotalForDate(selectedDate)
            
            val fileName = "Taxi_Dia_${dateFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.csv"
            val content = generateDayReport(selectedDate)
            
            val result = saveReportToFile(fileName, content)
            return@withContext if (result != null) {
                ExportResult.Success(result, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo de exportación")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar datos diarios: ${e.message}", e)
            return@withContext ExportResult.Error(
                "Error al exportar datos diarios: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }
    
    /**
     * Obtiene el rango de la semana (desde el primer día de la semana hasta el último)
     * según la lógica requerida:
     * - La semana comienza en el día configurado por el usuario
     * - La semana termina el día anterior al configurado
     * - No se cruzan meses, al comenzar un mes se inicia una nueva semana
     */
    // Función eliminada - usar directamente DateUtils.getWeekRange
    
    /**
     * Exporta los datos financieros de la semana que contiene la fecha seleccionada
     * @param selectedDate Fecha seleccionada para exportar datos
     * @param firstDayOfWeek Primer día de la semana (1 = Domingo, 2 = Lunes, etc.)
     * @return ExportResult con el resultado de la operación
     */
    suspend fun exportWeekData(selectedDate: Date, firstDayOfWeek: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
            
            val content = generateWeekReport(startOfWeek, endOfWeek, firstDayOfWeek)
            
            val fileName = "Taxi_Semana_${dateFormat.format(startOfWeek)}_a_${dateFormat.format(endOfWeek)}_${fileNameDateFormat.format(Date())}.csv"
            
            val result = saveReportToFile(fileName, content)
            return@withContext if (result != null) {
                ExportResult.Success(result, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo de exportación semanal")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar datos semanales: ${e.message}", e)
            return@withContext ExportResult.Error(
                "Error al exportar datos semanales: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }
    
    /**
     * Exporta los datos financieros del mes que contiene la fecha seleccionada
     * @param selectedDate Fecha seleccionada para exportar datos
     * @return ExportResult con el resultado de la operación
     */
    suspend fun exportMonthData(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            // Inicio del mes
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.time
            
            // Fin del mes
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.time
            
            val content = generateMonthReport(startOfMonth, endOfMonth)
            
            // Formato más descriptivo para el nombre del archivo
            val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
            val fileName = "Taxi_Mes_${monthYearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.csv"
            
            val result = saveReportToFile(fileName, content)
            return@withContext if (result != null) {
                ExportResult.Success(result, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo de exportación mensual")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar datos mensuales: ${e.message}", e)
            return@withContext ExportResult.Error(
                "Error al exportar datos mensuales: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }
    
    private suspend fun generateDayReport(selectedDate: Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val rides = taxiRideRepository.getRidesForDate(selectedDate).first()
        val expenses = expenseRepository.getExpensesForDate(selectedDate).first()
        
        val totalIncome = taxiRideRepository.getIncomeForDate(selectedDate)
        val totalExpenses = expenseRepository.getTotalExpensesForDate(selectedDate)
        val profit = totalIncome - totalExpenses
        
        // Obtener ingresos por método de pago
        val incomeByPaymentMethod = taxiRideRepository.getIncomeByPaymentMethodForDate(selectedDate)
        
        val sb = StringBuilder()
        
        // Encabezado simple
        sb.appendLine("INFORME DIARIO: ${dateFormat.format(selectedDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine("RESUMEN")
        sb.appendLine("Ingresos,Gastos,Beneficio")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        // Desglose por método de pago simplificado
        if (incomeByPaymentMethod.isNotEmpty()) {
            sb.appendLine("METODOS DE PAGO")
            sb.appendLine("Metodo,Importe")
            
            incomeByPaymentMethod.forEach { (method, amount) ->
                sb.appendLine("${method},${formatMoney(amount)}")
            }
            sb.appendLine()
        }
        
        // Gastos simplificados
        if (expenses.isNotEmpty()) {
            // Calcular gastos por tipo
            val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
            val otherExpenses = expenses.filter { it.type == ExpenseType.OTHER }.sumOf { it.amount }
            
            // Resumen de gastos por tipo
            sb.appendLine("GASTOS POR TIPO")
            sb.appendLine("Tipo,Importe")
            sb.appendLine("Combustible,${formatMoney(fuelExpenses)}")
            sb.appendLine("Otros,${formatMoney(otherExpenses)}")
            sb.appendLine()
            
            // Detalle de gastos simplificado
            sb.appendLine("GASTOS")
            sb.appendLine("Hora,Descripcion,Importe,Tipo")
            
            expenses.sortedBy { it.date }
                .forEach { expense ->
                    val safeDescription = (expense.description ?: "").replace(",", ";")
                    val safeType = expense.type.toString().replace(",", ";")
                    
                    sb.appendLine("${dateTimeFormat.format(expense.date)},${safeDescription},${formatMoney(expense.amount)},${safeType}")
                }
            sb.appendLine()
        }
        
        // Carreras simplificadas
        if (rides.isNotEmpty()) {
            sb.appendLine("CARRERAS")
            sb.appendLine("Hora,Importe,Metodo de Pago")
            
            rides.sortedBy { it.date }
                .forEach { ride ->
                    val safePaymentMethod = ride.paymentMethod.replace(",", ";")
                    
                    sb.appendLine("${dateTimeFormat.format(ride.date)},${formatMoney(ride.price)},${safePaymentMethod}")
                }
        }
        
        return sb.toString()
    }
    
    /**
     * Genera un informe semanal en formato CSV simplificado
     */
    private suspend fun generateWeekReport(startDate: Date, endDate: Date, firstDayOfWeek: Int): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        
        // Obtener ingresos por método de pago para la semana
        val incomeByPaymentMethod = rides
            .groupBy { it.paymentMethod }
            .mapValues { (_, rides) -> rides.sumOf { it.price } }
        
        // Calcular gastos por tipo
        val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val otherExpenses = expenses.filter { it.type == ExpenseType.OTHER }.sumOf { it.amount }
        
        val sb = StringBuilder()
        
        // Encabezado simplificado
        sb.appendLine("INFORME SEMANAL: ${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine("RESUMEN")
        sb.appendLine("Ingresos,Gastos,Beneficio")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        // Desglose por método de pago simplificado
        if (incomeByPaymentMethod.isNotEmpty()) {
            sb.appendLine("METODOS DE PAGO")
            sb.appendLine("Metodo,Importe")
            
            incomeByPaymentMethod.forEach { (method, amount) ->
                sb.appendLine("${method},${formatMoney(amount)}")
            }
            sb.appendLine()
        }
        
        // Resumen de gastos por tipo simplificado
        sb.appendLine("GASTOS POR TIPO")
        sb.appendLine("Tipo,Importe")
        sb.appendLine("Combustible,${formatMoney(fuelExpenses)}")
        sb.appendLine("Otros,${formatMoney(otherExpenses)}")
        sb.appendLine()
        
        // Resumen por día simplificado
        sb.appendLine("RESUMEN DIARIO")
        sb.appendLine("Fecha,Ingresos,Gastos,Beneficio")
        
        // Crear un mapa para agrupar por día
        val dailyData = mutableMapOf<String, Triple<Double, Double, Double>>()
        
        // Agrupar ingresos por día
        rides.forEach { ride ->
            val dateStr = dateFormat.format(ride.date)
            val current = dailyData.getOrDefault(dateStr, Triple(0.0, 0.0, 0.0))
            dailyData[dateStr] = Triple(current.first + ride.price, current.second, current.third)
        }
        
        // Agrupar gastos por día
        expenses.forEach { expense ->
            val dateStr = dateFormat.format(expense.date)
            val current = dailyData.getOrDefault(dateStr, Triple(0.0, 0.0, 0.0))
            dailyData[dateStr] = Triple(current.first, current.second + expense.amount, current.third)
        }
        
        // Calcular beneficio y añadir al informe
        dailyData.entries.sortedBy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.key) }.forEach { (date, data) ->
            val benefit = data.first - data.second
            sb.appendLine("$date,${formatMoney(data.first)},${formatMoney(data.second)},${formatMoney(benefit)}")
        }
        
        return sb.toString()
    }
    
    /**
     * Genera un informe mensual en formato CSV simplificado
     */
    private suspend fun generateMonthReport(startDate: Date, endDate: Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        
        // Obtener ingresos por método de pago para el mes
        val incomeByPaymentMethod = rides
            .groupBy { it.paymentMethod }
            .mapValues { (_, rides) -> rides.sumOf { it.price } }
        
        // Calcular gastos por tipo
        val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val otherExpenses = expenses.filter { it.type == ExpenseType.OTHER }.sumOf { it.amount }
        
        val sb = StringBuilder()
        
        // Encabezado simplificado
        sb.appendLine("INFORME MENSUAL: ${monthYearFormat.format(startDate).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}")
        sb.appendLine("${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine("RESUMEN")
        sb.appendLine("Ingresos,Gastos,Beneficio")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        // Desglose por método de pago simplificado
        if (incomeByPaymentMethod.isNotEmpty()) {
            sb.appendLine("METODOS DE PAGO")
            sb.appendLine("Metodo,Importe")
            
            incomeByPaymentMethod.forEach { (method, amount) ->
                sb.appendLine("${method},${formatMoney(amount)}")
            }
            sb.appendLine()
        }
        
        // Resumen de gastos por tipo simplificado
        sb.appendLine("GASTOS POR TIPO")
        sb.appendLine("Tipo,Importe")
        sb.appendLine("Combustible,${formatMoney(fuelExpenses)}")
        sb.appendLine("Otros,${formatMoney(otherExpenses)}")
        sb.appendLine()
        
        // Resumen semanal simplificado
        sb.appendLine("RESUMEN SEMANAL")
        sb.appendLine("Semana,Ingresos,Gastos,Beneficio")
        
        // Obtener el primer día de la semana configurado
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        // Crear un calendario para la primera fecha del mes
        val firstDayCalendar = Calendar.getInstance().apply { 
            time = startDate
            firstDayOfWeek = firstDayOfWeekValue
        }
        
        // Obtener el mes que estamos procesando
        val targetMonth = firstDayCalendar.get(Calendar.MONTH)
        val targetYear = firstDayCalendar.get(Calendar.YEAR)
        
        // Lista para almacenar los períodos de las semanas
        val weekPeriods = mutableListOf<Pair<Date, Date>>()
        
        // Caso 1: Si el día 1 del mes no coincide con el día seleccionado como inicio de semana
        // La primera semana va desde el día 1 hasta el día anterior al inicio de semana seleccionado
        val dayOfWeekOfFirstDay = firstDayCalendar.get(Calendar.DAY_OF_WEEK)
        
        if (dayOfWeekOfFirstDay != firstDayOfWeekValue) {
            // Primer período especial: desde el día 1 hasta el día anterior al seleccionado como inicio
            val firstWeekStart = firstDayCalendar.clone() as Calendar
            val firstWeekEnd = firstDayCalendar.clone() as Calendar
            
            // Avanzar hasta encontrar el primer día seleccionado como inicio de semana
            while (firstWeekEnd.get(Calendar.DAY_OF_WEEK) != firstDayOfWeekValue) {
                firstWeekEnd.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            // Retroceder un día para que termine en el día anterior al seleccionado
            firstWeekEnd.add(Calendar.DAY_OF_MONTH, -1)
            firstWeekEnd.set(Calendar.HOUR_OF_DAY, 23)
            firstWeekEnd.set(Calendar.MINUTE, 59)
            firstWeekEnd.set(Calendar.SECOND, 59)
            
            // Solo añadir si sigue dentro del mes
            if (firstWeekEnd.get(Calendar.MONTH) == targetMonth && 
                firstWeekEnd.get(Calendar.YEAR) == targetYear) {
                weekPeriods.add(Pair(firstWeekStart.time, firstWeekEnd.time))
            }
            
            // Comenzar desde el primer día seleccionado como inicio de semana
            firstDayCalendar.time = firstWeekEnd.time
            firstDayCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Generar periodos de semanas completas que comienzan en el día seleccionado
        while (firstDayCalendar.get(Calendar.MONTH) == targetMonth && 
               firstDayCalendar.get(Calendar.YEAR) == targetYear) {
            val weekStart = firstDayCalendar.clone() as Calendar
            val weekEnd = firstDayCalendar.clone() as Calendar
            
            // Avanzar 6 días para completar una semana (día seleccionado + 6 días)
            weekEnd.add(Calendar.DAY_OF_MONTH, 6)
            weekEnd.set(Calendar.HOUR_OF_DAY, 23)
            weekEnd.set(Calendar.MINUTE, 59)
            weekEnd.set(Calendar.SECOND, 59)
            
            // Si el fin de semana está fuera del mes, ajustarlo al último día del mes
            if (weekEnd.get(Calendar.MONTH) != targetMonth || 
                weekEnd.get(Calendar.YEAR) != targetYear) {
                
                // Establecer al último día del mes
                weekEnd.set(Calendar.DAY_OF_MONTH, 1)
                weekEnd.add(Calendar.MONTH, 1) 
                weekEnd.add(Calendar.DAY_OF_MONTH, -1)
                weekEnd.set(Calendar.HOUR_OF_DAY, 23)
                weekEnd.set(Calendar.MINUTE, 59)
                weekEnd.set(Calendar.SECOND, 59)
            }
            
            weekPeriods.add(Pair(weekStart.time, weekEnd.time))
            
            // Avanzar al siguiente inicio de semana (7 días después)
            firstDayCalendar.add(Calendar.DAY_OF_MONTH, 7)
        }
        
        // Crear un mapa para almacenar los datos por semana
        val weeklyData = mutableMapOf<Int, Triple<Double, Double, Double>>()
        
        // Procesar cada período
        weekPeriods.forEachIndexed { index, period ->
            // Filtrar ingresos para este período
            val periodRides = rides.filter { ride -> 
                ride.date >= period.first && ride.date <= period.second 
            }
            val periodIncome = periodRides.sumOf { it.price }
            
            // Filtrar gastos para este período
            val periodExpenses = expenses.filter { expense -> 
                expense.date >= period.first && expense.date <= period.second 
            }
            val periodExpenseAmount = periodExpenses.sumOf { it.amount }
            
            // Almacenar datos para este período
            weeklyData[index + 1] = Triple(periodIncome, periodExpenseAmount, 0.0)
        }
        
        // Calcular beneficio y añadir al informe
        weeklyData.entries.sortedBy { it.key }.forEach { (week, data) ->
            val benefit = data.first - data.second
            sb.appendLine("Semana $week,${formatMoney(data.first)},${formatMoney(data.second)},${formatMoney(benefit)}")
        }
        
        return sb.toString()
    }
    
    /**
     * Guarda el informe en un archivo CSV en el directorio de descargas
     * @return Uri del archivo guardado o null si ocurrió un error
     */
    private fun saveReportToFile(fileName: String, content: String): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para Android 10 (API 29) y superior, usamos MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("No se pudo crear el archivo en el almacenamiento externo")
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                    }
                } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
                
                return uri
            } else {
                // Para versiones anteriores a Android 10, usamos el método tradicional
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    throw IllegalStateException("No se pudo crear el directorio de descargas")
                }
                
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                    }
                }
                
                return Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Registrar el error de manera más detallada
            android.util.Log.e("ExportService", "Error al guardar el archivo: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Comparte un archivo a través de las aplicaciones disponibles en el dispositivo
     * @param uri Uri del archivo a compartir
     * @param fileName Nombre del archivo para mostrar en el selector de aplicaciones
     * @return ShareResult con el resultado de la operación
     */
    fun shareFile(uri: Uri, fileName: String): ShareResult {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Informe financiero Taxi")
                putExtra(Intent.EXTRA_TEXT, "Informe financiero generado por la aplicación Taxi: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Compartir informe a través de")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
            
            ShareResult.Success
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al compartir el archivo: ${e.message}", e)
            ShareResult.Error(
                "Error al compartir el archivo: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }
}
