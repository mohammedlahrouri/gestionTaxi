package com.moham.taxi.data.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.repository.ExpenseRepository
import com.moham.taxi.data.repository.TaxiRideRepository
import com.moham.taxi.utils.DateUtils
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
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
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val fileSafeDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private fun translatePaymentMethod(method: String): String {
        val lower = method.trim().lowercase()
        return when {
            lower == "efectivo" || lower == "cash" -> context.getString(R.string.payment_cash)
            lower == "tarjeta" || lower == "card" -> context.getString(R.string.payment_card)
            lower.replace(" ", "") == "viaapp" || lower == "via app" -> context.getString(R.string.payment_via_app)
            lower == "abonados" || lower == "subscribers" || lower == "account" -> context.getString(R.string.payment_subscribers)
            lower == "pendientes" || lower == "pending" -> context.getString(R.string.payment_pending)
            else -> method
        }
    }

    private fun translatePlatform(platform: String): String {
        val lower = platform.trim().lowercase()
        return when {
            lower == "directo" || lower == "direct" -> context.getString(R.string.platform_direct)
            else -> platform
        }
    }

    private fun formatServicesCount(count: Int): String {
        return if (count == 0) {
            context.getString(R.string.report_no_services)
        } else {
            context.getString(R.string.report_services_count, count)
        }
    }
    private val headerBackground = DeviceRgb(230, 235, 240)
    private val rowStripe = DeviceRgb(245, 247, 250)
    private val csvNumberFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
    
    /**
     * Formatea un valor monetario para exportación
     */
    private fun formatMoney(amount: Double): String {
        return com.moham.taxi.ui.components.formatCurrency(amount)
    }

    private fun formatCsvNumber(amount: Double): String {
        return csvNumberFormat.format(amount)
    }

    private fun csvField(value: String?): String {
        val v = (value ?: "").trim()
        if (v.isEmpty()) return ""
        val needsQuotes = v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return v
        val escaped = v.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun buildPeriodCsv(
        periodStart: Date,
        periodEnd: Date,
        rides: List<TaxiRide>,
        expenses: List<Expense>
    ): String {
        val dateOnly = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())

        val totalGross = rides.sumOf { it.price }
        val totalNet = rides.sumOf { getNetAmount(it) }
        val totalCommission = (totalGross - totalNet).coerceAtLeast(0.0)
        val totalExpenses = expenses.sumOf { it.amount }
        val profit = totalGross - totalExpenses

        val header = listOf(
            "row_type",
            "period_start",
            "period_end",
            "date",
            "time",
            "origin",
            "destination",
            "platform",
            "service_type",
            "payment_method",
            "gross",
            "net",
            "commission",
            "expense_type",
            "expense_description",
            "expense_amount"
        ).joinToString(",")

        val lines = ArrayList<String>(rides.size + expenses.size + 2)
        lines.add(header)

        lines.add(
            listOf(
                "SUMMARY",
                dateOnly.format(periodStart),
                dateOnly.format(periodEnd),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                formatCsvNumber(totalGross),
                formatCsvNumber(totalNet),
                formatCsvNumber(totalCommission),
                "",
                "${context.getString(R.string.report_expenses)} / ${context.getString(R.string.report_benefit)}",
                "${formatCsvNumber(totalExpenses)} / ${formatCsvNumber(profit)}"
            ).joinToString(",") { csvField(it) }
        )

        rides.sortedBy { it.date }.forEach { ride ->
            val serviceTypeLabel = when (ride.serviceType) {
                TaxiRide.SERVICE_TYPE_FIXED -> context.getString(R.string.option_fixed_price)
                else -> context.getString(R.string.option_taximeter)
            }
            val gross = ride.price
            val net = getNetAmount(ride)
            val commission = (gross - net).coerceAtLeast(0.0)
            lines.add(
                listOf(
                    "RIDE",
                    dateOnly.format(periodStart),
                    dateOnly.format(periodEnd),
                    dateOnly.format(ride.date),
                    ride.rideTime.ifBlank { timeOnly.format(ride.date) },
                    ride.origin,
                    ride.destination,
                    translatePlatform(ride.servicePlatform ?: ""),
                    serviceTypeLabel,
                    translatePaymentMethod(ride.paymentMethod),
                    formatCsvNumber(gross),
                    formatCsvNumber(net),
                    formatCsvNumber(commission),
                    "",
                    "",
                    ""
                ).joinToString(",") { csvField(it) }
            )
        }

        expenses.sortedBy { it.date }.forEach { expense ->
            val expenseTypeLabel = if (expense.type == ExpenseType.FUEL) context.getString(R.string.report_fuel) else context.getString(R.string.report_other)
            lines.add(
                listOf(
                    "EXPENSE",
                    dateOnly.format(periodStart),
                    dateOnly.format(periodEnd),
                    dateOnly.format(expense.date),
                    timeOnly.format(expense.date),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    expenseTypeLabel,
                    expense.description ?: "",
                    formatCsvNumber(expense.amount)
                ).joinToString(",") { csvField(it) }
            )
        }

        return lines.joinToString("\n")
    }

    private fun formatPercent(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f%%", value)
    }

    private fun getNetAmount(ride: TaxiRide): Double {
        return ride.netPrice ?: ride.price
    }

    private fun getCommissionAmount(ride: TaxiRide): Double {
        return (ride.price - getNetAmount(ride)).coerceAtLeast(0.0)
    }

    private fun getCommissionPercent(ride: TaxiRide, commissionAmount: Double): Double? {
        val netAmount = getNetAmount(ride)
        return ride.commissionPercentAtTime ?: if (commissionAmount > 0.0 && netAmount > 0.0) {
            commissionAmount / netAmount * 100.0
        } else {
            null
        }
    }

    private fun buildCommissionLabel(ride: TaxiRide): String {
        val commissionAmount = getCommissionAmount(ride)
        val commissionPercent = getCommissionPercent(ride, commissionAmount)
        val vatPercent = ride.commissionVatAtTime
        if (commissionAmount <= 0.0 && commissionPercent == null && vatPercent == null) {
            return "-"
        }
        val percentLabel = commissionPercent?.let { formatPercent(it) }
        val vatLabel = vatPercent?.let { formatPercent(it) }
        val vatSuffix = context.getString(R.string.vat_short)
        val percentText = when {
            percentLabel != null && vatLabel != null -> "$percentLabel + $vatLabel $vatSuffix"
            percentLabel != null -> percentLabel
            vatLabel != null -> "$vatLabel $vatSuffix"
            else -> "-"
        }
        return if (commissionAmount > 0.0) {
            if (percentText == "-") {
                formatMoney(commissionAmount)
            } else {
                "$percentText (${formatMoney(commissionAmount)})"
            }
        } else {
            percentText
        }
    }

    private fun buildTripText(ride: TaxiRide): String {
        val origin = ride.origin.ifBlank { "-" }
        val destination = ride.destination.ifBlank { "-" }
        return "$origin\n$destination"
    }

    private fun buildPaymentPlatformText(ride: TaxiRide): String {
        val payment = translatePaymentMethod(ride.paymentMethod.ifBlank { "-" })
        val platform = ride.servicePlatform?.ifBlank { "Directo" } ?: "Directo"
        val translatedPlatform = translatePlatform(platform)
        return if (platform.equals("Directo", ignoreCase = true)) {
            payment
        } else {
            "$payment\n$translatedPlatform"
        }
    }

    private fun buildAmountsText(ride: TaxiRide): String {
        val netValue = getNetAmount(ride)
        val commissionLabel = buildCommissionLabel(ride)
        return "${context.getString(R.string.report_gross_short)}: ${formatMoney(ride.price)}\n${context.getString(R.string.report_commission_short)}: $commissionLabel\n${context.getString(R.string.report_net_short)}: ${formatMoney(netValue)}"
    }

    private fun resolveLogoResId(): Int {
        val candidates = listOf("ic_launcher_foreground", "ic_launcher")
        candidates.forEach { name ->
            val mipmapId = context.resources.getIdentifier(name, "mipmap", context.packageName)
            if (mipmapId != 0) return mipmapId
            val drawableId = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (drawableId != 0) return drawableId
        }
        return 0
    }

    private fun loadLogoImage(): Image? {
        val resId = resolveLogoResId()
        if (resId == 0) return null
        val bitmap = BitmapFactory.decodeResource(context.resources, resId) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        val imageData = ImageDataFactory.create(stream.toByteArray())
        return Image(imageData).setHeight(36f)
    }

    private suspend fun addReportHeader(document: Document, title: String, subtitle: String?) {
        val application = context.applicationContext as GestionTaxiApplication
        val billingData = application.getBillingData().first()
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(1.4f, 2.6f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)

        val leftCell = Cell().setBorder(Border.NO_BORDER)
        loadLogoImage()?.let { leftCell.add(it) }
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        leftCell.add(Paragraph(appName).setBold().setFontSize(12f))
        leftCell.add(Paragraph(title).setBold().setFontSize(14f))
        subtitle?.let { leftCell.add(Paragraph(it).setFontSize(10f)) }

        val rightCell = Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
        val billingLines = listOfNotNull(
            billingData.name.takeIf { it.isNotBlank() },
            billingData.nif.takeIf { it.isNotBlank() }?.let { "NIF: $it" },
            billingData.license.takeIf { it.isNotBlank() }?.let { "Licencia: $it" },
            billingData.street.takeIf { it.isNotBlank() },
            listOf(billingData.postalCode, billingData.city).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "" }
        ).filter { it.isNotBlank() }

        if (billingLines.isEmpty()) {
            rightCell.add(Paragraph("Datos del conductor no configurados").setFontSize(9f))
        } else {
            billingLines.forEach { rightCell.add(Paragraph(it).setFontSize(9f)) }
        }

        headerTable.addCell(leftCell)
        headerTable.addCell(rightCell)
        document.add(headerTable)
        document.add(Paragraph(" "))
    }
    
    /**
     * Exporta los datos financieros del día seleccionado
     * @param selectedDate Fecha seleccionada para exportar datos
     * @return ExportResult con el resultado de la operación
     */
    suspend fun exportDayData(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = "Taxi_Dia_${fileSafeDateFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.pdf"
            val result = savePdfToFile(fileName) { document ->
                generateDayReportPdf(document, selectedDate)
            }
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

    suspend fun exportDayDataCsv(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val (start, end) = DateUtils.getDayRange(selectedDate)
            val rides = taxiRideRepository.getRidesForDate(selectedDate).first()
            val expenses = expenseRepository.getExpensesForDate(selectedDate).first()
            val csv = buildPeriodCsv(start, end, rides, expenses)

            val fileName = "Taxi_Dia_${fileSafeDateFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.csv"
            val uri = saveTextToFile(fileName, "text/csv", csv)
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo CSV de exportación")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar CSV diario: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar CSV diario: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
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
            
            val fileName = "Taxi_Semana_${fileSafeDateFormat.format(startOfWeek)}_a_${fileSafeDateFormat.format(endOfWeek)}_${fileNameDateFormat.format(Date())}.pdf"
            val result = savePdfToFile(fileName) { document ->
                generateWeekReportPdf(document, startOfWeek, endOfWeek, firstDayOfWeek)
            }
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

    suspend fun exportWeekDataCsv(selectedDate: Date, firstDayOfWeek: Int): ExportResult = withContext(Dispatchers.IO) {
        try {
            val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
            val rides = taxiRideRepository.getTaxiRidesByDateRange(startOfWeek, endOfWeek).first()
            val expenses = expenseRepository.getExpensesByDateRange(startOfWeek, endOfWeek).first()
            val csv = buildPeriodCsv(startOfWeek, endOfWeek, rides, expenses)

            val fileName = "Taxi_Semana_${fileSafeDateFormat.format(startOfWeek)}_a_${fileSafeDateFormat.format(endOfWeek)}_${fileNameDateFormat.format(Date())}.csv"
            val uri = saveTextToFile(fileName, "text/csv", csv)
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo CSV de exportación semanal")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar CSV semanal: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar CSV semanal: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
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
            
            val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
            val fileName = "Taxi_Mes_${monthYearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.pdf"
            
            val result = savePdfToFile(fileName) { document ->
                generateMonthReportPdf(document, startOfMonth, endOfMonth)
            }
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

    suspend fun exportMonthDataCsv(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.time

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.time

            val rides = taxiRideRepository.getTaxiRidesByDateRange(startOfMonth, endOfMonth).first()
            val expenses = expenseRepository.getExpensesByDateRange(startOfMonth, endOfMonth).first()
            val csv = buildPeriodCsv(startOfMonth, endOfMonth, rides, expenses)

            val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
            val fileName = "Taxi_Mes_${monthYearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.csv"
            val uri = saveTextToFile(fileName, "text/csv", csv)
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo CSV de exportación mensual")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar CSV mensual: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar CSV mensual: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    suspend fun exportYearData(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYear = calendar.time
            
            calendar.set(Calendar.MONTH, Calendar.DECEMBER)
            calendar.set(Calendar.DAY_OF_MONTH, 31)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfYear = calendar.time
            
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            val fileName = "Taxi_Anio_${yearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.pdf"
            
            val result = savePdfToFile(fileName) { document ->
                generateYearReportPdf(document, startOfYear, endOfYear)
            }
            return@withContext if (result != null) {
                ExportResult.Success(result, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo de exportación anual")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar datos anuales: ${e.message}", e)
            return@withContext ExportResult.Error(
                "Error al exportar datos anuales: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    suspend fun exportYearDataCsv(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYear = calendar.time

            calendar.set(Calendar.MONTH, Calendar.DECEMBER)
            calendar.set(Calendar.DAY_OF_MONTH, 31)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfYear = calendar.time

            val rides = taxiRideRepository.getTaxiRidesByDateRange(startOfYear, endOfYear).first()
            val expenses = expenseRepository.getExpensesByDateRange(startOfYear, endOfYear).first()
            val csv = buildPeriodCsv(startOfYear, endOfYear, rides, expenses)

            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            val fileName = "Taxi_Anio_${yearFormat.format(selectedDate)}_${fileNameDateFormat.format(Date())}.csv"
            val uri = saveTextToFile(fileName, "text/csv", csv)
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo CSV de exportación anual")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar CSV anual: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar CSV anual: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    suspend fun exportRangeData(startDate: Date, endDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val fileName = "Taxi_Rango_${fileSafeDateFormat.format(startDate)}_a_${fileSafeDateFormat.format(endDate)}_${fileNameDateFormat.format(Date())}.pdf"
            val uri = savePdfToFile(fileName) { document ->
                generateRangeReportPdf(document, startDate, endDate)
            }
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo de exportación por rango")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar rango PDF: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar rango PDF: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    suspend fun exportRangeDataCsv(startDate: Date, endDate: Date): ExportResult = withContext(Dispatchers.IO) {
        try {
            val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
            val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
            val csv = buildPeriodCsv(startDate, endDate, rides, expenses)

            val fileName = "Taxi_Rango_${fileSafeDateFormat.format(startDate)}_a_${fileSafeDateFormat.format(endDate)}_${fileNameDateFormat.format(Date())}.csv"
            val uri = saveTextToFile(fileName, "text/csv", csv)
            return@withContext if (uri != null) {
                ExportResult.Success(uri, fileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo CSV de exportación por rango")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar rango CSV: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar rango CSV: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    private suspend fun exportTickets(startDate: Date, endDate: Date, fileNamePrefix: String): ExportResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ExportService", "exportTickets: startDate=$startDate (${startDate.time}), endDate=$endDate (${endDate.time}), fileNamePrefix=$fileNamePrefix")
            
            val rides = taxiRideRepository.getTaxiRidesByDateRangeSuspend(startDate, endDate)
            val expenses = expenseRepository.getExpensesByDateRangeSuspend(startDate, endDate)
            
            android.util.Log.d("ExportService", "Found ${rides.size} rides and ${expenses.size} expenses in range")
            android.util.Log.d("ExportService", "Ride dates: ${rides.map { "${it.date} (${it.date.time})" }}")
            android.util.Log.d("ExportService", "Expense dates: ${expenses.map { "${it.date} (${it.date.time})" }}")
            
            val tempDir = File(context.cacheDir, "tickets_export_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val folderExpenses = context.getString(R.string.zip_expenses)
            val folderFuel = context.getString(R.string.zip_fuel)
            val folderOther = context.getString(R.string.zip_other)
            val folderRides = context.getString(R.string.zip_rides)

            val gastosDir = File(tempDir, folderExpenses)
            val combustibleDir = File(gastosDir, folderFuel)
            val otrosDir = File(gastosDir, folderOther)
            val carrerasDir = File(tempDir, folderRides)

            expenses.forEach { expense ->
                val photoPath = expense.ticketPhotoPath ?: run {
                    android.util.Log.d("ExportService", "Skipping expense ${expense.id} - no photo path")
                    return@forEach
                }
                android.util.Log.d("ExportService", "Processing expense ${expense.id}, photoPath: $photoPath")
                val photoFile = com.moham.taxi.utils.ImageUtils.getTicketPhotoFile(context, photoPath) ?: run {
                    android.util.Log.w("ExportService", "Expense photo not found: $photoPath")
                    return@forEach
                }
                if (!photoFile.exists()) {
                    android.util.Log.w("ExportService", "Expense photo file does not exist: ${photoFile.absolutePath}")
                    return@forEach
                }

                val targetDir = if (expense.type == ExpenseType.FUEL) combustibleDir else otrosDir
                targetDir.mkdirs()
                val targetFile = File(targetDir, photoFile.name)
                try {
                    photoFile.copyTo(targetFile, overwrite = true)
                    android.util.Log.d("ExportService", "Copied expense photo to ${targetFile.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.e("ExportService", "Failed to copy expense photo", e)
                }
            }

            rides.forEach { ride ->
                val photoPath = ride.ticketPhotoPath ?: run {
                    android.util.Log.d("ExportService", "Skipping ride ${ride.id} - no photo path")
                    return@forEach
                }
                android.util.Log.d("ExportService", "Processing ride ${ride.id}, photoPath: $photoPath")
                val photoFile = com.moham.taxi.utils.ImageUtils.getTicketPhotoFile(context, photoPath) ?: run {
                    android.util.Log.w("ExportService", "Ride photo not found: $photoPath")
                    return@forEach
                }
                if (!photoFile.exists()) {
                    android.util.Log.w("ExportService", "Ride photo file does not exist: ${photoFile.absolutePath}")
                    return@forEach
                }

                val platform = ride.servicePlatform ?: "Directo"
                val paymentMethod = ride.paymentMethod
                val translatedPlatform = translatePlatform(platform)
                val translatedPayment = translatePaymentMethod(paymentMethod)
                val platformDir = File(carrerasDir, translatedPlatform)
                val paymentDir = File(platformDir, translatedPayment)
                paymentDir.mkdirs()
                val targetFile = File(paymentDir, photoFile.name)
                try {
                    photoFile.copyTo(targetFile, overwrite = true)
                    android.util.Log.d("ExportService", "Copied ride photo to ${targetFile.absolutePath}")
                } catch (e: Exception) {
                    android.util.Log.e("ExportService", "Failed to copy ride photo", e)
                }
            }

            val zipFileName = "${fileNamePrefix}_${fileNameDateFormat.format(Date())}.zip"
            val zipFile = File(context.cacheDir, zipFileName)
            zipFile.delete()

            zipDirectory(tempDir, zipFile)
            
            android.util.Log.d("ExportService", "Created ZIP file: ${zipFile.absolutePath}, size=${zipFile.length()} bytes")

            val uri = saveZipToFile(zipFileName, zipFile)
            tempDir.deleteRecursively()
            zipFile.delete()

            return@withContext if (uri != null) {
                ExportResult.Success(uri, zipFileName)
            } else {
                ExportResult.Error("No se pudo guardar el archivo ZIP de tickets")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExportService", "Error al exportar tickets: ${e.message}", e)
            ExportResult.Error(
                "Error al exportar tickets: ${e.localizedMessage ?: e.message ?: "Error desconocido"}",
                e
            )
        }
    }

    private fun zipDirectory(sourceDir: File, zipFile: File) {
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = sourceDir.toPath().relativize(file.toPath()).toString()
                val zipEntry = java.util.zip.ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun saveZipToFile(fileName: String, zipFile: File): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("No se pudo crear el archivo ZIP en el almacenamiento externo")
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    zipFile.inputStream().use { it.copyTo(outputStream) }
                } ?: throw IllegalStateException("No se pudo abrir el archivo ZIP para escritura")
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    throw IllegalStateException("No se pudo crear el directorio de descargas")
                }
                val file = File(downloadsDir, fileName)
                zipFile.copyTo(file, overwrite = true)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ExportService", "Error al guardar el archivo ZIP: ${e.message}", e)
            null
        }
    }

    suspend fun exportDayDataTickets(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        val (start, end) = DateUtils.getDayRange(selectedDate)
        val fileNamePrefix = "Taxi_Tickets_Dia_${fileSafeDateFormat.format(selectedDate)}"
        return@withContext exportTickets(start, end, fileNamePrefix)
    }

    suspend fun exportWeekDataTickets(selectedDate: Date, firstDayOfWeek: Int): ExportResult = withContext(Dispatchers.IO) {
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
        val fileNamePrefix = "Taxi_Tickets_Semana_${fileSafeDateFormat.format(startOfWeek)}_a_${fileSafeDateFormat.format(endOfWeek)}"
        return@withContext exportTickets(startOfWeek, endOfWeek, fileNamePrefix)
    }

    suspend fun exportMonthDataTickets(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time
        
        val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
        val fileNamePrefix = "Taxi_Tickets_Mes_${monthYearFormat.format(selectedDate)}"
        return@withContext exportTickets(startOfMonth, endOfMonth, fileNamePrefix)
    }

    suspend fun exportYearDataTickets(selectedDate: Date): ExportResult = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfYear = calendar.time
        
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfYear = calendar.time
        
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val fileNamePrefix = "Taxi_Tickets_Anio_${yearFormat.format(selectedDate)}"
        return@withContext exportTickets(startOfYear, endOfYear, fileNamePrefix)
    }

    suspend fun exportRangeDataTickets(startDate: Date, endDate: Date): ExportResult = withContext(Dispatchers.IO) {
        val fileNamePrefix = "Taxi_Tickets_Rango_${fileSafeDateFormat.format(startDate)}_a_${fileSafeDateFormat.format(endDate)}"
        return@withContext exportTickets(startDate, endDate, fileNamePrefix)
    }
    
    private suspend fun generateDayReportPdf(document: Document, selectedDate: Date) {
        val rides = taxiRideRepository.getRidesForDate(selectedDate).first()
        val expenses = expenseRepository.getExpensesForDate(selectedDate).first()
        val totalIncome = taxiRideRepository.getIncomeForDate(selectedDate)
        val totalExpenses = expenseRepository.getTotalExpensesForDate(selectedDate)
        val profit = totalIncome - totalExpenses
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val dayRange = DateUtils.getDayRange(selectedDate)
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(dayRange.first, dayRange.second)
        val platformData = taxiRideRepository.getIncomeByPlatform(dayRange.first, dayRange.second)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(dayRange.first, dayRange.second)

        addReportHeader(document, "Informe diario", dateFormat.format(selectedDate))
        addSummarySection(document, totalIncome, totalExpenses, profit)
        addPaymentMethodSectionPdf(document, incomeByPaymentMethod, appPlatformData)
        addServiceTypeSection(document, rides)
        addPlatformSectionPdf(document, platformData, withoutPlatform)
        addExpensesSectionPdf(document, expenses)
        addDailyRidesSectionPdf(document, rides)
        addDailyTotalsFooter(document, rides)
        addDisclaimerPdf(document)
    }

    private suspend fun generateWeekReportPdf(
        document: Document,
        startDate: Date,
        endDate: Date,
        firstDayOfWeek: Int
    ) {
        document.pdfDocument.defaultPageSize = PageSize.A4.rotate()
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)

        addReportHeader(
            document,
            "Informe semanal",
            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        )
        addSummarySection(document, totalIncome, totalExpenses, profit)
        addPaymentMethodSectionPdf(document, incomeByPaymentMethod, appPlatformData)
        addServiceTypeSection(document, rides)
        addPlatformSectionPdf(document, platformData, withoutPlatform)
        addExpensesSectionPdf(document, expenses)
        addWeeklyGroupedRidesSectionPdf(document, rides, startDate, endDate)
        addWeeklyTotalsFooter(document, rides)
        addDisclaimerPdf(document)
    }

    private suspend fun generateRangeReportPdf(document: Document, startDate: Date, endDate: Date) {
        document.pdfDocument.defaultPageSize = PageSize.A4.rotate()
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)

        addReportHeader(
            document,
            "Informe por rango",
            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        )
        addSummarySection(document, totalIncome, totalExpenses, profit)
        addPaymentMethodSectionPdf(document, incomeByPaymentMethod, appPlatformData)
        addServiceTypeSection(document, rides)
        addPlatformSectionPdf(document, platformData, withoutPlatform)
        addExpensesSectionPdf(document, expenses)
        addRangeGroupedRidesSectionPdf(document, rides, startDate, endDate)
        addRangeTotalsFooter(document, rides)
        addDisclaimerPdf(document)
    }

    private suspend fun generateMonthReportPdf(document: Document, startDate: Date, endDate: Date) {
        document.pdfDocument.defaultPageSize = PageSize.A4.rotate()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

        addReportHeader(
            document,
            "Informe mensual: ${monthYearFormat.format(startDate).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        )
        addSummarySection(document, totalIncome, totalExpenses, profit)
        addPaymentMethodSectionPdf(document, incomeByPaymentMethod, appPlatformData)
        addServiceTypeSection(document, rides)
        addPlatformSectionPdf(document, platformData, withoutPlatform)
        addExpensesSectionPdf(document, expenses)
        addMonthlyWeeklySummarySectionPdf(document, rides, startDate, firstDayOfWeekValue)
        addMonthlyDailyPerformanceSectionPdf(document, rides, expenses, startDate, endDate)
        addMonthlyWeeklyDetailsSectionPdf(document, rides, startDate, firstDayOfWeekValue)
        addDisclaimerPdf(document)
    }

    private suspend fun generateYearReportPdf(document: Document, startDate: Date, endDate: Date) {
        document.pdfDocument.defaultPageSize = PageSize.A4.rotate()
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)
        val yearFormat = SimpleDateFormat("yyyy", Locale("es", "ES"))

        addReportHeader(
            document,
            "Informe anual: ${yearFormat.format(startDate)}",
            "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
        )
        addSummarySection(document, totalIncome, totalExpenses, profit)
        addPaymentMethodSectionPdf(document, incomeByPaymentMethod, appPlatformData)
        addServiceTypeSection(document, rides)
        addPlatformSectionPdf(document, platformData, withoutPlatform)
        addExpensesSectionPdf(document, expenses)
        addRangeGroupedRidesSectionPdf(document, rides, startDate, endDate)
        addRangeTotalsFooter(document, rides)
        addDisclaimerPdf(document)
    }

    private fun addTitle(document: Document, title: String) {
        document.add(
            Paragraph(title)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16f)
        )
        document.add(Paragraph(" "))
    }

    private fun addSummarySection(document: Document, totalIncome: Double, totalExpenses: Double, profit: Double) {
        document.add(Paragraph(context.getString(R.string.report_financial_summary)).setBold())
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addTableRow(table, context.getString(R.string.report_income), formatMoney(totalIncome))
        addTableRow(table, context.getString(R.string.report_expenses), formatMoney(totalExpenses))
        addTableRow(table, context.getString(R.string.report_net), formatMoney(profit))
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addPaymentMethodSectionPdf(
        document: Document,
        incomeByPaymentMethod: Map<String, Pair<Double, Int>>,
        appPlatformData: Map<String, Pair<Double, Int>>
    ) {
        if (incomeByPaymentMethod.isEmpty()) {
            return
        }

        document.add(Paragraph(context.getString(R.string.report_breakdown_by_payment_methods)).setBold())

        val viaAppEntry = incomeByPaymentMethod.entries.firstOrNull { it.key.equals("Via App", ignoreCase = true) }
        val remainingEntries = incomeByPaymentMethod.entries
            .filterNot { it.key.equals("Via App", ignoreCase = true) }
            .sortedByDescending { it.value.first }

        val orderedEntries = listOfNotNull(viaAppEntry) + remainingEntries

        orderedEntries.forEach { (method, data) ->
            val (income, count) = data
            val translatedMethod = translatePaymentMethod(method)
            document.add(Paragraph("${translatedMethod} ....................... ${formatMoney(income)} (${formatServicesCount(count)})"))

            if (method.equals("Via App", ignoreCase = true) && appPlatformData.isNotEmpty()) {
                val sortedPlatforms = appPlatformData.entries.sortedByDescending { it.value.first }
                sortedPlatforms.forEachIndexed { index, entry ->
                    val (platform, platformData) = entry
                    val (platformIncome, platformCount) = platformData
                    val prefix = if (index < sortedPlatforms.size - 1) "├─ " else "└─ "
                    val translatedPlatform = translatePlatform(platform)
                    document.add(
                        Paragraph("$prefix$translatedPlatform ....................... ${formatMoney(platformIncome)} (${formatServicesCount(platformCount)})")
                            .setMarginLeft(16f)
                            .setFontSize(10f)
                    )
                }
            }
        }
        document.add(Paragraph(" "))
    }

    private fun addServiceTypeSection(document: Document, rides: List<TaxiRide>) {
        val meterTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_METER }.sumOf { it.price }
        val fixedTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_FIXED }.sumOf { it.price }
        document.add(Paragraph(context.getString(R.string.report_breakdown_by_service_type)).setBold())
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addTableRow(table, context.getString(R.string.option_taximeter), formatMoney(meterTotal))
        addTableRow(table, context.getString(R.string.option_fixed_price), formatMoney(fixedTotal))
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addPlatformSectionPdf(
        document: Document,
        platformData: Map<String, Pair<Double, Int>>,
        withoutPlatform: Pair<Double, Int>
    ) {
        document.add(Paragraph(context.getString(R.string.report_breakdown_by_platform)).setBold())
        platformData.entries.sortedByDescending { it.value.first }.forEach { (platform, data) ->
            val (income, count) = data
            val translatedPlatform = translatePlatform(platform)
            document.add(Paragraph("${translatedPlatform} ....................... ${formatMoney(income)} (${formatServicesCount(count)})"))
        }
        val totalWithPlatform = platformData.values.sumOf { it.first }
        val totalWithPlatformCount = platformData.values.sumOf { it.second }
        document.add(Paragraph("${context.getString(R.string.report_total_with_platform)} ....................... ${formatMoney(totalWithPlatform)} (${formatServicesCount(totalWithPlatformCount)})"))
        if (withoutPlatform.second > 0) {
            document.add(Paragraph("${context.getString(R.string.report_no_platform)} ....................... ${formatMoney(withoutPlatform.first)} (${formatServicesCount(withoutPlatform.second)})"))
        }
        document.add(Paragraph(" "))
    }

    private fun addExpensesSectionPdf(document: Document, expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            return
        }

        val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val otherExpenses = expenses.filter { it.type != ExpenseType.FUEL }.sumOf { it.amount }

        document.add(Paragraph(context.getString(R.string.report_detailed_expenses)).setBold())
        val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addTableRow(summaryTable, context.getString(R.string.report_fuel), formatMoney(fuelExpenses))
        addTableRow(summaryTable, context.getString(R.string.report_other), formatMoney(otherExpenses))
        document.add(summaryTable)
        document.add(Paragraph(" "))

        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 4f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_concept_date))
        addHeaderCell(table, context.getString(R.string.report_concept_description))
        addHeaderCell(table, context.getString(R.string.report_concept_amount))
        addHeaderCell(table, context.getString(R.string.report_concept_type))

        expenses.sortedBy { it.date }.forEachIndexed { index, expense ->
            val description = (expense.description ?: "").ifBlank { "-" }
            addCell(table, dateTimeFormat.format(expense.date), index)
            addCell(table, description, index)
            addCell(table, formatMoney(expense.amount), index)
            val typeText = if (expense.type == ExpenseType.FUEL) context.getString(R.string.report_fuel) else context.getString(R.string.report_other)
            addCell(table, typeText, index)
        }
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addDailyRidesSectionPdf(document: Document, rides: List<TaxiRide>) {
        if (rides.isEmpty()) {
            document.add(Paragraph(context.getString(R.string.report_rides)).setBold())
            document.add(Paragraph(context.getString(R.string.report_no_rides)).setFontSize(10f))
            document.add(Paragraph(" "))
            return
        }

        document.add(Paragraph(context.getString(R.string.report_rides)).setBold())
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2.6f, 1.6f, 1.6f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_time))
        addHeaderCell(table, context.getString(R.string.report_trip))
        addHeaderCell(table, context.getString(R.string.report_platform_payment))
        addHeaderCell(table, context.getString(R.string.report_amounts))

        rides.sortedBy { it.date }.forEachIndexed { index, ride ->
            addCell(table, ride.rideTime.ifBlank { timeFormat.format(ride.date) }, index)
            addCell(table, buildTripText(ride), index)
            addCell(table, buildPaymentPlatformText(ride), index)
            addCell(table, buildAmountsText(ride), index)
        }
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addDailyTotalsFooter(document: Document, rides: List<TaxiRide>) {
        val totalGross = rides.sumOf { it.price }
        val totalNet = rides.sumOf { getNetAmount(it) }
        val totalCommission = (totalGross - totalNet).coerceAtLeast(0.0)
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_total_gross))
        addHeaderCell(table, context.getString(R.string.report_total_commission))
        addHeaderCell(table, context.getString(R.string.report_total_net))
        val rowIndex = 0
        addCell(table, formatMoney(totalGross), rowIndex, 11f)
        addCell(table, formatMoney(totalCommission), rowIndex, 11f)
        addCell(table, formatMoney(totalNet), rowIndex, 11f)
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addWeeklyGroupedRidesSectionPdf(document: Document, rides: List<TaxiRide>, startDate: Date, endDate: Date) {
        document.add(Paragraph(context.getString(R.string.report_services_by_day)).setBold())
        if (rides.isEmpty()) {
            document.add(Paragraph(context.getString(R.string.report_no_rides)).setFontSize(10f))
            document.add(Paragraph(" "))
            return
        }
        val dayFormat = SimpleDateFormat("EEEE dd", Locale.getDefault())
        val grouped = rides.groupBy { dateFormat.format(it.date) }
        val dayKeys = grouped.keys.mapNotNull { key ->
            runCatching { dateFormat.parse(key) }.getOrNull()?.let { key to it }
        }.sortedBy { it.second.time }

        dayKeys.forEach { (key, date) ->
            val dayRides = grouped[key].orEmpty()
            val dayLabel = dayFormat.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val dayGross = dayRides.sumOf { it.price }
            val dayNet = dayRides.sumOf { getNetAmount(it) }
            val dayCommission = (dayGross - dayNet).coerceAtLeast(0.0)
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 1f, 1f)))
                .setWidth(UnitValue.createPercentValue(100f))
            addHeaderCell(summaryTable, context.getString(R.string.report_day))
            addHeaderCell(summaryTable, context.getString(R.string.report_services))
            addHeaderCell(summaryTable, context.getString(R.string.report_gross_short))
            addHeaderCell(summaryTable, context.getString(R.string.report_commission_short))
            addHeaderCell(summaryTable, context.getString(R.string.report_net_short))
            addCell(summaryTable, dayLabel, 0)
            addCell(summaryTable, dayRides.size.toString(), 0)
            addCell(summaryTable, formatMoney(dayGross), 0)
            addCell(summaryTable, formatMoney(dayCommission), 0)
            addCell(summaryTable, formatMoney(dayNet), 0)
            document.add(summaryTable)

            val detailTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2.4f, 1.6f, 1.6f)))
                .setWidth(UnitValue.createPercentValue(98f))
            addHeaderCell(detailTable, context.getString(R.string.report_time))
            addHeaderCell(detailTable, context.getString(R.string.report_trip))
            addHeaderCell(detailTable, context.getString(R.string.report_platform_payment))
            addHeaderCell(detailTable, context.getString(R.string.report_amounts))
            dayRides.sortedBy { it.date }.forEachIndexed { index, ride ->
                addCell(detailTable, ride.rideTime.ifBlank { timeFormat.format(ride.date) }, index)
                addCell(detailTable, buildTripText(ride), index)
                addCell(detailTable, buildPaymentPlatformText(ride), index)
                addCell(detailTable, buildAmountsText(ride), index)
            }
            detailTable.setMarginLeft(8f)
            document.add(detailTable)
            document.add(Paragraph(" "))
        }

        if (rides.isNotEmpty()) {
            val rangeText = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
            document.add(Paragraph(context.getString(R.string.report_weekly_summary, rangeText)).setBold())
        }
    }

    private fun addRangeGroupedRidesSectionPdf(document: Document, rides: List<TaxiRide>, startDate: Date, endDate: Date) {
        document.add(Paragraph(context.getString(R.string.report_services_by_day)).setBold())
        if (rides.isEmpty()) {
            document.add(Paragraph(context.getString(R.string.report_no_rides)).setFontSize(10f))
            document.add(Paragraph(" "))
            return
        }
        val dayFormat = SimpleDateFormat("EEEE dd", Locale.getDefault())
        val grouped = rides.groupBy { dateFormat.format(it.date) }
        val dayKeys = grouped.keys.mapNotNull { key ->
            runCatching { dateFormat.parse(key) }.getOrNull()?.let { key to it }
        }.sortedBy { it.second.time }

        dayKeys.forEach { (key, date) ->
            val dayRides = grouped[key].orEmpty()
            val dayLabel = dayFormat.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val dayGross = dayRides.sumOf { it.price }
            val dayNet = dayRides.sumOf { getNetAmount(it) }
            val dayCommission = (dayGross - dayNet).coerceAtLeast(0.0)
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 1f, 1f)))
                .setWidth(UnitValue.createPercentValue(100f))
            addHeaderCell(summaryTable, context.getString(R.string.report_day))
            addHeaderCell(summaryTable, context.getString(R.string.report_services))
            addHeaderCell(summaryTable, context.getString(R.string.report_gross_short))
            addHeaderCell(summaryTable, context.getString(R.string.report_commission_short))
            addHeaderCell(summaryTable, context.getString(R.string.report_net_short))
            addCell(summaryTable, dayLabel, 0)
            addCell(summaryTable, dayRides.size.toString(), 0)
            addCell(summaryTable, formatMoney(dayGross), 0)
            addCell(summaryTable, formatMoney(dayCommission), 0)
            addCell(summaryTable, formatMoney(dayNet), 0)
            document.add(summaryTable)

            val detailTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2.4f, 1.6f, 1.6f)))
                .setWidth(UnitValue.createPercentValue(98f))
            addHeaderCell(detailTable, context.getString(R.string.report_time))
            addHeaderCell(detailTable, context.getString(R.string.report_trip))
            addHeaderCell(detailTable, context.getString(R.string.report_platform_payment))
            addHeaderCell(detailTable, context.getString(R.string.report_amounts))
            dayRides.sortedBy { it.date }.forEachIndexed { index, ride ->
                addCell(detailTable, ride.rideTime.ifBlank { timeFormat.format(ride.date) }, index)
                addCell(detailTable, buildTripText(ride), index)
                addCell(detailTable, buildPaymentPlatformText(ride), index)
                addCell(detailTable, buildAmountsText(ride), index)
            }
            detailTable.setMarginLeft(8f)
            document.add(detailTable)
            document.add(Paragraph(" "))
        }

        if (rides.isNotEmpty()) {
            val rangeText = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
            document.add(Paragraph(context.getString(R.string.report_summary_range, rangeText)).setBold())
        }
    }

    private fun addRangeTotalsFooter(document: Document, rides: List<TaxiRide>) {
        val totalGross = rides.sumOf { it.price }
        val totalNet = rides.sumOf { getNetAmount(it) }
        val totalCommission = (totalGross - totalNet).coerceAtLeast(0.0)
        val totalServices = rides.size
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1.4f, 1f, 1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_total_range))
        addHeaderCell(table, context.getString(R.string.report_services))
        addHeaderCell(table, context.getString(R.string.report_gross_short))
        addHeaderCell(table, context.getString(R.string.report_commission_short))
        addHeaderCell(table, context.getString(R.string.report_net_short))
        addCell(table, context.getString(R.string.report_range_short), 0, 11f)
        addCell(table, totalServices.toString(), 0, 11f)
        addCell(table, formatMoney(totalGross), 0, 11f)
        addCell(table, formatMoney(totalCommission), 0, 11f)
        addCell(table, formatMoney(totalNet), 0, 11f)
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addWeeklyTotalsFooter(document: Document, rides: List<TaxiRide>) {
        val totalGross = rides.sumOf { it.price }
        val totalNet = rides.sumOf { getNetAmount(it) }
        val totalCommission = (totalGross - totalNet).coerceAtLeast(0.0)
        val totalServices = rides.size
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1.4f, 1f, 1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_total_week))
        addHeaderCell(table, context.getString(R.string.report_services))
        addHeaderCell(table, context.getString(R.string.report_gross_short))
        addHeaderCell(table, context.getString(R.string.report_commission_short))
        addHeaderCell(table, context.getString(R.string.report_net_short))
        addCell(table, context.getString(R.string.report_week_short), 0, 11f)
        addCell(table, totalServices.toString(), 0, 11f)
        addCell(table, formatMoney(totalGross), 0, 11f)
        addCell(table, formatMoney(totalCommission), 0, 11f)
        addCell(table, formatMoney(totalNet), 0, 11f)
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addMonthlyWeeklySummarySectionPdf(
        document: Document,
        rides: List<TaxiRide>,
        startDate: Date,
        firstDayOfWeekValue: Int
    ) {
        document.add(Paragraph(context.getString(R.string.report_weekly_summary_title)).setBold())
        val weekCount = DateUtils.getMonthWeekCount(startDate, firstDayOfWeekValue)
        val weeklyTotals = mutableListOf<Pair<String, Triple<Double, Double, Int>>>()

        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f, 1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_week_short))
        addHeaderCell(table, context.getString(R.string.report_services))
        addHeaderCell(table, context.getString(R.string.report_gross_short))
        addHeaderCell(table, context.getString(R.string.report_commission_short))
        addHeaderCell(table, context.getString(R.string.report_net_short))

        (0 until weekCount).forEachIndexed { index, weekNumber ->
            val (weekStart, weekEnd) = DateUtils.getMonthWeekRange(startDate, weekNumber, firstDayOfWeekValue)
            val weekRides = rides.filter { it.date.time in weekStart.time..weekEnd.time }
            val weekGross = weekRides.sumOf { it.price }
            val weekNet = weekRides.sumOf { getNetAmount(it) }
            val weekCommission = (weekGross - weekNet).coerceAtLeast(0.0)
            val label = "${context.getString(R.string.report_week_short)} ${weekNumber + 1} (${dateFormat.format(weekStart)} - ${dateFormat.format(weekEnd)})"
            addCell(table, label, index)
            addCell(table, weekRides.size.toString(), index)
            addCell(table, formatMoney(weekGross), index)
            addCell(table, formatMoney(weekCommission), index)
            addCell(table, formatMoney(weekNet), index)
            weeklyTotals.add(label to Triple(weekGross, weekCommission, weekRides.size))
        }
        document.add(table)

        val maxGross = weeklyTotals.maxOfOrNull { it.second.first } ?: 0.0
        if (maxGross > 0.0) {
            document.add(Paragraph(context.getString(R.string.report_weekly_chart_title)).setBold().setFontSize(10f))
            weeklyTotals.forEach { (label, totals) ->
                val barLength = ((totals.first / maxGross) * 20).toInt().coerceAtLeast(1)
                val bar = "█".repeat(barLength)
                document.add(Paragraph("$label  $bar ${formatMoney(totals.first)}").setFontSize(9f))
            }
        }
        document.add(Paragraph(" "))
    }

    private fun addMonthlyDailyPerformanceSectionPdf(
        document: Document,
        rides: List<TaxiRide>,
        expenses: List<Expense>,
        startDate: Date,
        endDate: Date
    ) {
        document.add(Paragraph(context.getString(R.string.report_daily_performance)).setBold())
        val ridesByDay = rides.groupBy { dateFormat.format(it.date) }
        val expensesByDay = expenses.groupBy { dateFormat.format(it.date) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val table = Table(UnitValue.createPercentArray(floatArrayOf(1.4f, 1f, 1f, 1f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))
        addHeaderCell(table, context.getString(R.string.report_concept_date))
        addHeaderCell(table, context.getString(R.string.report_services))
        addHeaderCell(table, context.getString(R.string.report_gross_short))
        addHeaderCell(table, context.getString(R.string.report_expenses))
        addHeaderCell(table, context.getString(R.string.report_net_benefit))

        val calendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var rowIndex = 0
        while (calendar.time <= endDate) {
            val dateKey = dateFormat.format(calendar.time)
            val dayRides = ridesByDay[dateKey].orEmpty()
            val dayGross = dayRides.sumOf { it.price }
            val dayNet = dayRides.sumOf { getNetAmount(it) }
            val dayExpenses = expensesByDay[dateKey] ?: 0.0
            val dayBenefit = dayNet - dayExpenses
            addCell(table, dateKey, rowIndex)
            addCell(table, dayRides.size.toString(), rowIndex)
            addCell(table, formatMoney(dayGross), rowIndex)
            addCell(table, formatMoney(dayExpenses), rowIndex)
            addCell(table, formatMoney(dayBenefit), rowIndex)
            rowIndex++
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        document.add(table)
        document.add(Paragraph(" "))
    }

    private fun addMonthlyWeeklyDetailsSectionPdf(
        document: Document,
        rides: List<TaxiRide>,
        startDate: Date,
        firstDayOfWeekValue: Int
    ) {
        document.add(Paragraph(context.getString(R.string.report_detail_by_week)).setBold())
        val weekCount = DateUtils.getMonthWeekCount(startDate, firstDayOfWeekValue)
        (0 until weekCount).forEach { weekNumber ->
            val (weekStart, weekEnd) = DateUtils.getMonthWeekRange(startDate, weekNumber, firstDayOfWeekValue)
            val weekRides = rides.filter { it.date.time in weekStart.time..weekEnd.time }
            val label = "${context.getString(R.string.report_week_short)} ${weekNumber + 1} (${dateFormat.format(weekStart)} - ${dateFormat.format(weekEnd)})"
            document.add(Paragraph(label).setBold().setFontSize(11f))
            if (weekRides.isEmpty()) {
                document.add(Paragraph(context.getString(R.string.report_no_services)).setFontSize(9f))
                document.add(Paragraph(" "))
                return@forEach
            }
            val table = Table(UnitValue.createPercentArray(floatArrayOf(1.2f, 1f, 2.2f, 1.6f, 1.6f)))
                .setWidth(UnitValue.createPercentValue(100f))
            addHeaderCell(table, context.getString(R.string.report_concept_date))
            addHeaderCell(table, context.getString(R.string.report_time))
            addHeaderCell(table, context.getString(R.string.report_trip))
            addHeaderCell(table, context.getString(R.string.report_platform_payment))
            addHeaderCell(table, context.getString(R.string.report_amounts))
            weekRides.sortedBy { it.date }.forEachIndexed { index, ride ->
                addCell(table, dateFormat.format(ride.date), index)
                addCell(table, ride.rideTime.ifBlank { timeFormat.format(ride.date) }, index)
                addCell(table, buildTripText(ride), index)
                addCell(table, buildPaymentPlatformText(ride), index)
                addCell(table, buildAmountsText(ride), index)
            }
            document.add(table)
            document.add(Paragraph(" "))
        }
    }

    private fun addDisclaimerPdf(document: Document) {
        val disclaimer = """
${context.getString(R.string.report_disclaimer_title)}

${context.getString(R.string.report_disclaimer_body)}
        """.trimIndent()

        document.add(
            Paragraph(disclaimer)
                .setFontSize(10f)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY)
        )
    }

    private fun addTableRow(table: Table, label: String, value: String) {
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(label)))
        table.addCell(com.itextpdf.layout.element.Cell().add(Paragraph(value)))
    }

    private fun addHeaderCell(table: Table, text: String) {
        table.addCell(
            Cell()
                .add(Paragraph(text).setBold().setFontSize(10f))
                .setBackgroundColor(headerBackground)
                .setPadding(6f)
        )
    }

    private fun addCell(table: Table, text: String, rowIndex: Int, fontSize: Float = 9f) {
        val cell = Cell()
            .add(Paragraph(text).setFontSize(fontSize))
            .setPadding(5f)
        if (rowIndex % 2 == 1) {
            cell.setBackgroundColor(rowStripe)
        }
        table.addCell(cell)
    }

    private suspend fun generateDayReport(selectedDate: Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val rides = taxiRideRepository.getRidesForDate(selectedDate).first()
        val expenses = expenseRepository.getExpensesForDate(selectedDate).first()
        
        val totalIncome = taxiRideRepository.getIncomeForDate(selectedDate)
        val totalExpenses = expenseRepository.getTotalExpensesForDate(selectedDate)
        val profit = totalIncome - totalExpenses
        
        val incomeByPaymentMethod = rides.groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val dayRange = DateUtils.getDayRange(selectedDate)
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(dayRange.first, dayRange.second)
        val platformData = taxiRideRepository.getIncomeByPlatform(dayRange.first, dayRange.second)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(dayRange.first, dayRange.second)
        
        val sb = StringBuilder()
        
        // Encabezado simple
        sb.appendLine("${context.getString(R.string.report_daily).uppercase(Locale.getDefault())}: ${dateFormat.format(selectedDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine(context.getString(R.string.home_summary_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_income)},${context.getString(R.string.report_expenses)},${context.getString(R.string.report_benefit)}")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        if (incomeByPaymentMethod.isNotEmpty()) {
            appendPaymentMethodSection(sb, incomeByPaymentMethod, appPlatformData)
        }
        
        // Carreras simplificadas
        if (rides.isNotEmpty()) {
            sb.appendLine(context.getString(R.string.report_rides).uppercase(Locale.getDefault()))
            sb.appendLine("${context.getString(R.string.report_time)},${context.getString(R.string.label_origin)},${context.getString(R.string.label_destination)},${context.getString(R.string.report_concept_amount)},${context.getString(R.string.label_payment_method)},${context.getString(R.string.label_service_type)}")
            
            rides.sortedBy { it.date }
                .forEach { ride ->
                    val safeOrigin = ride.origin.replace(",", ";")
                    val safeDestination = ride.destination.replace(",", ";")
                    val safePaymentMethod = translatePaymentMethod(ride.paymentMethod).replace(",", ";")
                    val serviceTypeLabel = when (ride.serviceType) {
                        TaxiRide.SERVICE_TYPE_METER -> context.getString(R.string.option_taximeter)
                        TaxiRide.SERVICE_TYPE_FIXED -> context.getString(R.string.option_fixed_price)
                        else -> ""
                    }
                    val safeServiceType = serviceTypeLabel.replace(",", ";")
                    
                    sb.appendLine("${dateTimeFormat.format(ride.date)},${safeOrigin},${safeDestination},${formatMoney(ride.price)},${safePaymentMethod},${safeServiceType}")
                }
        }

        val meterTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_METER }.sumOf { it.price }
        val fixedTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_FIXED }.sumOf { it.price }
        sb.appendLine()
        sb.appendLine(context.getString(R.string.report_breakdown_by_service_type).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.option_taximeter)},${formatMoney(meterTotal)}")
        sb.appendLine("${context.getString(R.string.option_fixed_price)},${formatMoney(fixedTotal)}")
        sb.appendLine()

        appendPlatformSection(sb, platformData, withoutPlatform)

        if (expenses.isNotEmpty()) {
            val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
            val otherExpenses = expenses.filter { it.type != ExpenseType.FUEL }.sumOf { it.amount }
            
            sb.appendLine(context.getString(R.string.report_expenses_by_type_title).uppercase(Locale.getDefault()))
            sb.appendLine("${context.getString(R.string.report_type_header)},${context.getString(R.string.report_concept_amount)}")
            sb.appendLine("${context.getString(R.string.report_fuel)},${formatMoney(fuelExpenses)}")
            sb.appendLine("${context.getString(R.string.report_other)},${formatMoney(otherExpenses)}")
            sb.appendLine()
            
            sb.appendLine(context.getString(R.string.report_expenses).uppercase(Locale.getDefault()))
            sb.appendLine("${context.getString(R.string.report_time)},${context.getString(R.string.report_concept_description)},${context.getString(R.string.report_concept_amount)},${context.getString(R.string.report_concept_type)}")
            
            expenses.sortedBy { it.date }
                .forEach { expense ->
                    val safeDescription = (expense.description ?: "").replace(",", ";")
                    val typeLabel = if (expense.type == ExpenseType.FUEL) context.getString(R.string.report_fuel) else context.getString(R.string.report_other)
                    val safeType = typeLabel.replace(",", ";")
                    
                    sb.appendLine("${dateTimeFormat.format(expense.date)},${safeDescription},${formatMoney(expense.amount)},${safeType}")
                }
            sb.appendLine()
        }
        
        appendDisclaimerSection(sb)
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
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)
        
        // Calcular gastos por tipo
        val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val otherExpenses = expenses.filter { it.type != ExpenseType.FUEL }.sumOf { it.amount }
        
        val sb = StringBuilder()
        
        // Encabezado simplificado
        sb.appendLine("${context.getString(R.string.report_weekly).uppercase(Locale.getDefault())}: ${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine(context.getString(R.string.home_summary_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_income)},${context.getString(R.string.report_expenses)},${context.getString(R.string.report_benefit)}")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        if (incomeByPaymentMethod.isNotEmpty()) {
            appendPaymentMethodSection(sb, incomeByPaymentMethod, appPlatformData)
        }

        val meterTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_METER }.sumOf { it.price }
        val fixedTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_FIXED }.sumOf { it.price }
        sb.appendLine()
        sb.appendLine(context.getString(R.string.report_breakdown_by_service_type).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.option_taximeter)},${formatMoney(meterTotal)}")
        sb.appendLine("${context.getString(R.string.option_fixed_price)},${formatMoney(fixedTotal)}")
        sb.appendLine()

        appendPlatformSection(sb, platformData, withoutPlatform)

        sb.appendLine(context.getString(R.string.report_expenses_by_type_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_type_header)},${context.getString(R.string.report_concept_amount)}")
        sb.appendLine("${context.getString(R.string.report_fuel)},${formatMoney(fuelExpenses)}")
        sb.appendLine("${context.getString(R.string.report_other)},${formatMoney(otherExpenses)}")
        sb.appendLine()
        
        // Resumen por día simplificado
        sb.appendLine(context.getString(R.string.report_daily_performance).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_concept_date)},${context.getString(R.string.report_income)},${context.getString(R.string.report_expenses)},${context.getString(R.string.report_benefit)}")
        
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

        appendDisclaimerSection(sb)
        return sb.toString()
    }
    
    /**
     * Genera un informe mensual en formato CSV simplificado
     */
    private suspend fun generateMonthReport(startDate: Date, endDate: Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        val rides = taxiRideRepository.getTaxiRidesByDateRange(startDate, endDate).first()
        val expenses = expenseRepository.getExpensesByDateRange(startDate, endDate).first()
        
        val totalIncome = taxiRideRepository.getTotalIncomeByDateRange(startDate, endDate)
        val totalExpenses = expenseRepository.getTotalExpensesByDateRange(startDate, endDate)
        val profit = totalIncome - totalExpenses
        
        // Obtener ingresos por método de pago para el mes
        val incomeByPaymentMethod = rides
            .groupBy { it.paymentMethod }
            .mapValues { (_, items) -> items.sumOf { it.price } to items.size }
        val appPlatformData = taxiRideRepository.getAppIncomeByPlatform(startDate, endDate)
        val platformData = taxiRideRepository.getIncomeByPlatform(startDate, endDate)
        val withoutPlatform = taxiRideRepository.getIncomeWithoutPlatform(startDate, endDate)
        
        // Calcular gastos por tipo
        val fuelExpenses = expenses.filter { it.type == ExpenseType.FUEL }.sumOf { it.amount }
        val otherExpenses = expenses.filter { it.type != ExpenseType.FUEL }.sumOf { it.amount }
        
        val sb = StringBuilder()
        
        // Encabezado simplificado
        val formattedMonth = monthYearFormat.format(startDate).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        sb.appendLine(context.getString(R.string.report_monthly, formattedMonth).uppercase(Locale.getDefault()))
        sb.appendLine("${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}")
        sb.appendLine()
        
        // Resumen financiero simplificado
        sb.appendLine(context.getString(R.string.home_summary_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_income)},${context.getString(R.string.report_expenses)},${context.getString(R.string.report_benefit)}")
        sb.appendLine("${formatMoney(totalIncome)},${formatMoney(totalExpenses)},${formatMoney(profit)}")
        sb.appendLine()
        
        if (incomeByPaymentMethod.isNotEmpty()) {
            appendPaymentMethodSection(sb, incomeByPaymentMethod, appPlatformData)
        }

        val meterTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_METER }.sumOf { it.price }
        val fixedTotal = rides.filter { it.serviceType == TaxiRide.SERVICE_TYPE_FIXED }.sumOf { it.price }
        sb.appendLine()
        sb.appendLine(context.getString(R.string.report_breakdown_by_service_type).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.option_taximeter)},${formatMoney(meterTotal)}")
        sb.appendLine("${context.getString(R.string.option_fixed_price)},${formatMoney(fixedTotal)}")
        sb.appendLine()

        appendPlatformSection(sb, platformData, withoutPlatform)

        sb.appendLine(context.getString(R.string.report_expenses_by_type_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_type_header)},${context.getString(R.string.report_concept_amount)}")
        sb.appendLine("${context.getString(R.string.report_fuel)},${formatMoney(fuelExpenses)}")
        sb.appendLine("${context.getString(R.string.report_other)},${formatMoney(otherExpenses)}")
        sb.appendLine()
        
        // Resumen semanal simplificado
        sb.appendLine(context.getString(R.string.report_weekly_summary_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_week)},${context.getString(R.string.report_income)},${context.getString(R.string.report_expenses)},${context.getString(R.string.report_benefit)}")
        
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
            sb.appendLine("${context.getString(R.string.report_week)} $week,${formatMoney(data.first)},${formatMoney(data.second)},${formatMoney(benefit)}")
        }

        appendDisclaimerSection(sb)
        return sb.toString()
    }
    
    private fun appendPaymentMethodSection(
        sb: StringBuilder,
        incomeByPaymentMethod: Map<String, Pair<Double, Int>>,
        appPlatformData: Map<String, Pair<Double, Int>>
    ) {
        sb.appendLine(context.getString(R.string.report_payment_methods_title).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.report_method_header)},${context.getString(R.string.report_concept_amount)},${context.getString(R.string.report_services)}")
        
        val viaAppEntry = incomeByPaymentMethod.entries.firstOrNull { 
            it.key.equals("Via App", ignoreCase = true) || it.key.equals(context.getString(R.string.payment_via_app), ignoreCase = true) 
        }
        val remainingEntries = incomeByPaymentMethod.entries
            .filterNot { 
                it.key.equals("Via App", ignoreCase = true) || it.key.equals(context.getString(R.string.payment_via_app), ignoreCase = true) 
            }
            .sortedByDescending { it.value.first }

        val orderedEntries = listOfNotNull(viaAppEntry) + remainingEntries

        orderedEntries.forEach { (method, data) ->
            val (income, count) = data
            val translatedMethod = translatePaymentMethod(method)
            sb.appendLine("${translatedMethod},${formatMoney(income)},${count}")

            if ((method.equals("Via App", ignoreCase = true) || method.equals(context.getString(R.string.payment_via_app), ignoreCase = true)) && appPlatformData.isNotEmpty()) {
                val sortedPlatforms = appPlatformData.entries.sortedByDescending { it.value.first }
                sortedPlatforms.forEachIndexed { index, entry ->
                    val (platform, platformData) = entry
                    val (platformIncome, platformCount) = platformData
                    val prefix = if (index < sortedPlatforms.size - 1) "  ├─ " else "  └─ "
                    val translatedPlatform = translatePlatform(platform)
                    sb.appendLine("${prefix}${translatedPlatform},${formatMoney(platformIncome)},${platformCount}")
                }
            }
        }
        sb.appendLine()
    }

    private fun appendPlatformSection(
        sb: StringBuilder,
        platformData: Map<String, Pair<Double, Int>>,
        withoutPlatform: Pair<Double, Int>
    ) {
        sb.appendLine(context.getString(R.string.report_breakdown_by_platform).uppercase(Locale.getDefault()))
        sb.appendLine("${context.getString(R.string.label_platform)},${context.getString(R.string.report_concept_amount)},${context.getString(R.string.report_services)}")
        
        platformData.entries.sortedByDescending { it.value.first }.forEach { (platform, data) ->
            val (income, count) = data
            val translatedPlatform = translatePlatform(platform)
            sb.appendLine("${translatedPlatform},${formatMoney(income)},${count}")
        }
        
        val totalWithPlatform = platformData.values.sumOf { it.first }
        val totalWithPlatformCount = platformData.values.sumOf { it.second }
        sb.appendLine("${context.getString(R.string.report_total_with_platform)},${formatMoney(totalWithPlatform)},${totalWithPlatformCount}")
        
        if (withoutPlatform.second > 0) {
            sb.appendLine("${context.getString(R.string.report_no_platform)},${formatMoney(withoutPlatform.first)},${withoutPlatform.second}")
        }
        sb.appendLine()
    }

    private fun appendDisclaimerSection(sb: StringBuilder) {
        sb.appendLine()
        appendQuotedLine(sb, context.getString(R.string.report_disclaimer_title))
        appendQuotedLine(sb, context.getString(R.string.report_disclaimer_body))
    }

    private fun appendQuotedLine(sb: StringBuilder, text: String) {
        val escaped = text.replace("\"", "\"\"")
        sb.appendLine("\"$escaped\"")
    }
    
    private suspend fun savePdfToFile(fileName: String, build: suspend (Document) -> Unit): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("No se pudo crear el archivo en el almacenamiento externo")
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = PdfWriter(outputStream)
                    val pdf = PdfDocument(writer)
                    Document(pdf).use { document ->
                        build(document)
                    }
                } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
                
                return uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    throw IllegalStateException("No se pudo crear el directorio de descargas")
                }
                
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    val writer = PdfWriter(fos)
                    val pdf = PdfDocument(writer)
                    Document(pdf).use { document ->
                        build(document)
                    }
                }
                
                return Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ExportService", "Error al guardar el archivo: ${e.message}", e)
            return null
        }
    }

    private fun saveTextToFile(fileName: String, mimeType: String, content: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("No se pudo crear el archivo en el almacenamiento externo")
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("No se pudo abrir el archivo para escritura")
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    throw IllegalStateException("No se pudo crear el directorio de descargas")
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ExportService", "Error al guardar el archivo de texto: ${e.message}", e)
            null
        }
    }
    
    /**
     * Comparte un archivo a través de las aplicaciones disponibles en el dispositivo
     * @param uri Uri del archivo a compartir
     * @param fileName Nombre del archivo para mostrar en el selector de aplicaciones
     * @return ShareResult con el resultado de la operación
     */
    fun shareFile(uri: Uri, fileName: String, mimeType: String = "application/pdf"): ShareResult {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_subject))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.report_body, fileName))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.report_share_title))
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
