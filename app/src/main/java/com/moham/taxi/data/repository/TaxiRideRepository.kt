package com.moham.taxi.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.dao.TaxiRideDao
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.MonthlySummary
import com.moham.taxi.data.model.YearlySummary
import com.moham.taxi.data.model.PeriodSummary
import com.moham.taxi.data.model.ServicePlatformSummary
import com.moham.taxi.data.model.ServicePlatformCountSummary
import com.moham.taxi.data.model.IncomeCountSummary
import com.moham.taxi.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

// Repositorio actualizado para manejar correctamente Flow.first() y utilizar métodos directos del DAO
class TaxiRideRepository(private val taxiRideDao: TaxiRideDao, private val database: AppDatabase, private val context: Context) : BaseRepository() {
    
    // Sistema de caché mejorado para viajes frecuentes
    private val rideCache = ConcurrentHashMap<Long, CacheEntry<TaxiRide>>()
    private val dateRangeCache = ConcurrentHashMap<String, CacheEntry<List<TaxiRide>>>()
    private val incomeCache = ConcurrentHashMap<String, CacheEntry<Double>>()
    private val paginationCache = ConcurrentHashMap<String, CacheEntry<List<TaxiRide>>>()
    private val summaryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    // Las constantes de caché se heredan de BaseRepository
    
    /**
     * Enhanced cache cleanup that includes all cache types
     */
    private fun cleanupCache() {
        cleanupCacheMap(rideCache)
        cleanupCacheMap(dateRangeCache)
        cleanupCacheMap(incomeCache)
        cleanupCacheMap(paginationCache)
        
        // Clean summary cache with smaller threshold
        if (summaryCache.size > SUMMARY_CACHE_SIZE) {
            val entriesToRemove = summaryCache.size - (SUMMARY_CACHE_SIZE / 2)
            summaryCache.entries.sortedBy { it.value.timestamp }
                .take(entriesToRemove)
                .forEach { summaryCache.remove(it.key) }
        }
    }
    
    /**
     * Invalida todo el caché relacionado con rangos de fechas e ingresos
     */
    private fun invalidateDateRangeCache() {
        dateRangeCache.clear()
        incomeCache.clear()
    }
    
    /**
     * Genera una clave de caché para rangos de fechas
     */
    private fun getDateRangeKey(startDate: Date, endDate: Date): String {
        return "${startDate.time}_${endDate.time}"
    }
    
    fun getContext(): Context {
        return context
    }
    
    val allTaxiRides: Flow<List<TaxiRide>> = taxiRideDao.getAllTaxiRides()
    
    suspend fun insert(taxiRide: TaxiRide, triggerOnlineBackup: Boolean = true): Long = withContext(Dispatchers.IO) {
        val id = taxiRideDao.insert(taxiRide)
        rideCache[id] = CacheEntry(taxiRide.copy(id = id))
        invalidateDateRangeCache()
        cleanupCache()
        val application = context.applicationContext as GestionTaxiApplication
        application.savePendingBackup(true)
        id
    }
    
    suspend fun update(taxiRide: TaxiRide) = withContext(Dispatchers.IO) {
        taxiRideDao.update(taxiRide)
        rideCache[taxiRide.id] = CacheEntry(taxiRide)
        invalidateDateRangeCache()
        cleanupCache()
        val application = context.applicationContext as GestionTaxiApplication
        application.savePendingBackup(true)
    }
    
    suspend fun delete(taxiRide: TaxiRide) = withContext(Dispatchers.IO) {
        taxiRideDao.delete(taxiRide)
        rideCache.remove(taxiRide.id)
        invalidateDateRangeCache()
        cleanupCache()
        val application = context.applicationContext as GestionTaxiApplication
        application.savePendingBackup(true)
    }
    
    suspend fun insertMultiple(taxiRides: List<TaxiRide>): List<Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            taxiRides.chunked(BATCH_SIZE).flatMap { batch ->
                batch.map { taxiRide ->
                    val id = taxiRideDao.insert(taxiRide)
                    rideCache[id] = CacheEntry(taxiRide.copy(id = id))
                    id
                }
            }
        }.also {
            invalidateDateRangeCache()
            cleanupCache()
        }
    }
    
    suspend fun getTaxiRideById(id: Long): TaxiRide? {
        rideCache[id]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return cacheEntry.data
            } else {
                rideCache.remove(id)
            }
        }
        return taxiRideDao.getTaxiRideById(id)?.also { taxiRide ->
            rideCache[id] = CacheEntry(taxiRide)
        }
    }
    
    fun getTodayRides(): Flow<List<TaxiRide>> {
        val dayRange = com.moham.taxi.utils.DateUtils.getCurrentDayRange()
        return taxiRideDao.getTaxiRidesByDateRange(dayRange.first, dayRange.second)
    }
    
    fun getMonthRides(): Flow<List<TaxiRide>> {
        val monthRange = com.moham.taxi.utils.DateUtils.getCurrentMonthRange()
        return taxiRideDao.getTaxiRidesByDateRange(monthRange.first, monthRange.second)
    }
    
    suspend fun getTodayIncome(): Double {
        val dayRange = com.moham.taxi.utils.DateUtils.getCurrentDayRange()
        return taxiRideDao.getTotalIncomeByDateRange(dayRange.first, dayRange.second) ?: 0.0
    }
    
    suspend fun getMonthIncome(): Double {
        val monthRange = com.moham.taxi.utils.DateUtils.getCurrentMonthRange()
        return taxiRideDao.getTotalIncomeByDateRange(monthRange.first, monthRange.second) ?: 0.0
    }
    
    suspend fun getTodayIncomeByPaymentMethod(): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getCurrentDayRange()
        val summaries = taxiRideDao.getTotalByPaymentMethod(dayRange.first, dayRange.second)
        return summaries.associate { it.paymentMethod to it.total }
    }
    
    suspend fun getMonthIncomeByPaymentMethod(): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getCurrentMonthRange()
        val summaries = taxiRideDao.getTotalByPaymentMethod(monthRange.first, monthRange.second)
        return summaries.associate { it.paymentMethod to it.total }
    }
    
    fun getRidesForDate(date: Date): Flow<List<TaxiRide>> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        return taxiRideDao.getTaxiRidesByDateRange(dayRange.first, dayRange.second)
    }
    
    suspend fun getIncomeForDate(date: Date): Double {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        return taxiRideDao.getTotalIncomeByDateRange(dayRange.first, dayRange.second) ?: 0.0
    }

    suspend fun getTipsForDate(date: Date): Double {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        return taxiRideDao.getTotalTipsByDateRange(dayRange.first, dayRange.second) ?: 0.0
    }

    suspend fun getTipsByMethodForDate(date: Date): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        val summaries = taxiRideDao.getTipsByMethod(dayRange.first, dayRange.second)
        return summaries.associate { it.paymentMethod to it.total }
    }
    
    suspend fun getIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        val calendar = Calendar.getInstance().apply { 
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time
        
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.time
        
        val summaries = taxiRideDao.getTotalByPaymentMethod(startOfDay, endOfDay)
        return summaries.associate { it.paymentMethod to it.total }
    }

    suspend fun getAppIncomeByPlatform(startDate: Date, endDate: Date): Map<String, Pair<Double, Int>> {
        val summaries = taxiRideDao.getAppIncomeByPlatformWithCount(startDate, endDate)
        return mapPlatformCountSummaries(summaries)
    }

    suspend fun getIncomeByPlatform(startDate: Date, endDate: Date): Map<String, Pair<Double, Int>> {
        val summaries = taxiRideDao.getIncomeByPlatformWithCount(startDate, endDate)
        return mapPlatformCountSummaries(summaries)
    }

    suspend fun getIncomeWithoutPlatform(startDate: Date, endDate: Date): Pair<Double, Int> {
        val summary = taxiRideDao.getIncomeWithoutPlatform(startDate, endDate)
        return summary.total to summary.count
    }

    suspend fun getAppIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        val summaries = taxiRideDao.getAppIncomeByPlatform(dayRange.first, dayRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getAppNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        val summaries = taxiRideDao.getAppNetIncomeByPlatform(dayRange.first, dayRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        val summaries = taxiRideDao.getTotalIncomeByPlatform(dayRange.first, dayRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(date)
        val summaries = taxiRideDao.getNetIncomeByPlatform(dayRange.first, dayRange.second)
        return mapPlatformSummaries(summaries)
    }
    
    suspend fun getRideCountForDate(date: Date): Int {
        val calendar = Calendar.getInstance().apply { 
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time
        
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = calendar.time
        
        return taxiRideDao.getRideCountByDateRange(startOfDay, endOfDay) ?: 0
    }
    
    suspend fun getMonthRideCount(): Int {
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val endOfMonth = Calendar.getInstance().apply {
            val lastDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, lastDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
        
        return taxiRideDao.getRideCountByDateRange(startOfMonth, endOfMonth) ?: 0
    }
    
    suspend fun getTotalIncomeByDateRange(startDate: Date, endDate: Date): Double {
        return taxiRideDao.getTotalIncomeByDateRange(startDate, endDate) ?: 0.0
    }
    
    fun getTaxiRidesByDateRange(startDate: Date, endDate: Date): Flow<List<TaxiRide>> {
        return taxiRideDao.getTaxiRidesByDateRange(startDate, endDate)
    }
    
    suspend fun getTaxiRidesByDateRangeSuspend(startDate: Date, endDate: Date): List<TaxiRide> {
        return taxiRideDao.getTaxiRidesByDateRangeSuspend(startDate, endDate)
    }
    
    /**
     * Obtiene el total de ingresos de la semana actual.
     */
    suspend fun getCurrentWeekIncome(): Double {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        // Usar la función que ya hemos adaptado para obtener el rango de la semana actual
        return getWeekIncomeForDate(today)
    }
    
    /**
     * Obtiene el número de carreras de la semana actual.
     */
    suspend fun getCurrentWeekRideCount(): Int {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        val today = Date()
        // Usar DateUtils.getWeekRange para obtener el rango correcto de la semana actual
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(today, firstDayOfWeekValue)
        
        return taxiRideDao.getRideCountByDateRange(startOfWeek, endOfWeek) ?: 0
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para la semana actual.
     */
    suspend fun getCurrentWeekIncomeByPaymentMethod(): Map<String, Double> {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        val today = Date()
        // Usar DateUtils.getWeekRange para obtener el rango correcto de la semana actual
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(today, firstDayOfWeekValue)
        
        val summaries = taxiRideDao.getTotalByPaymentMethod(startOfWeek, endOfWeek)
        return summaries.associate { it.paymentMethod to it.total }
    }
    
    /**
     * Obtiene el ingreso de la semana de una fecha específica.
     */
    suspend fun getWeekIncomeForDate(date: Date): Double {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        // Obtener el rango de la semana usando la nueva lógica
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        
        return taxiRideDao.getTotalIncomeByDateRange(startOfWeek, endOfWeek) ?: 0.0
    }

    suspend fun getWeekTipsForDate(date: Date): Double {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        return taxiRideDao.getTotalTipsByDateRange(startOfWeek, endOfWeek) ?: 0.0
    }

    suspend fun getWeekTipsByMethodForDate(date: Date): Map<String, Double> {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        val summaries = taxiRideDao.getTipsByMethod(startOfWeek, endOfWeek)
        return summaries.associate { it.paymentMethod to it.total }
    }
    
    /**
     * Obtiene el número de carreras de la semana de una fecha específica.
     */
    suspend fun getWeekRideCountForDate(date: Date): Int {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        // Obtener el rango de la semana usando la nueva lógica
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        
        return taxiRideDao.getRideCountByDateRange(startOfWeek, endOfWeek) ?: 0
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para la semana de una fecha específica.
     */
    suspend fun getWeekIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        // Obtener el primer día de la semana configurado
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        
        // Obtener el rango de la semana usando la nueva lógica
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        
        val summaries = taxiRideDao.getTotalByPaymentMethod(startOfWeek, endOfWeek)
        return summaries.associate { it.paymentMethod to it.total }
    }

    suspend fun getWeekAppIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        val summaries = taxiRideDao.getAppIncomeByPlatform(startOfWeek, endOfWeek)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getWeekAppNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        val summaries = taxiRideDao.getAppNetIncomeByPlatform(startOfWeek, endOfWeek)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getWeekIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        val summaries = taxiRideDao.getTotalIncomeByPlatform(startOfWeek, endOfWeek)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getWeekNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeekValue = application.getFirstDayOfWeek().first()
        val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeekValue)
        val summaries = taxiRideDao.getNetIncomeByPlatform(startOfWeek, endOfWeek)
        return mapPlatformSummaries(summaries)
    }
    
    /**
     * Obtiene el ingreso del mes de una fecha específica.
     */
    suspend fun getMonthIncomeForDate(date: Date): Double {
        val calendar = Calendar.getInstance().apply { time = date }
        
        // Ajustar al primer día del mes
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        // Ajustar al último día del mes
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time
        
        println("DEBUG REPOSITORY: Calculando ingresos de mes desde ${startOfMonth} hasta ${endOfMonth}")
        
        return taxiRideDao.getTotalIncomeByDateRange(startOfMonth, endOfMonth) ?: 0.0
    }

    suspend fun getMonthTipsForDate(date: Date): Double {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        return taxiRideDao.getTotalTipsByDateRange(monthRange.first, monthRange.second) ?: 0.0
    }
    
    /**
     * Obtiene el número de carreras del mes de una fecha específica.
     */
    suspend fun getMonthRideCountForDate(date: Date): Int {
        val calendar = Calendar.getInstance().apply { time = date }
        
        // Ajustar al primer día del mes
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        // Ajustar al último día del mes
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.time
        
        return taxiRideDao.getRideCountByDateRange(startOfMonth, endOfMonth) ?: 0
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para el mes de una fecha específica.
     */
    suspend fun getMonthIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getTotalByPaymentMethod(monthRange.first, monthRange.second)
        return summaries.associate { it.paymentMethod to it.total }
    }

    suspend fun getMonthTipsByMethodForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getTipsByMethod(monthRange.first, monthRange.second)
        return summaries.associate { it.paymentMethod to it.total }
    }

    suspend fun getMonthAppIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getAppIncomeByPlatform(monthRange.first, monthRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getMonthAppNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getAppNetIncomeByPlatform(monthRange.first, monthRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getMonthIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getTotalIncomeByPlatform(monthRange.first, monthRange.second)
        return mapPlatformSummaries(summaries)
    }

    suspend fun getMonthNetIncomeByPlatformForDate(date: Date): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getMonthRange(date)
        val summaries = taxiRideDao.getNetIncomeByPlatform(monthRange.first, monthRange.second)
        return mapPlatformSummaries(summaries)
    }
    
    /**
     * Inserta una nueva carrera en la base de datos
     * @param taxiRide La carrera a insertar
     * @return El ID de la carrera insertada
     */
    suspend fun insertTaxiRide(taxiRide: TaxiRide): Long {
        return insert(taxiRide, triggerOnlineBackup = false)
    }
    
    /**
     * Elimina carreras en un rango de fechas específico
     * @param startDate Fecha de inicio del rango
     * @param endDate Fecha de fin del rango
     */
    suspend fun deleteTaxiRidesByDateRange(startDate: Date, endDate: Date) {
        taxiRideDao.deleteByDateRange(startDate, endDate)
    }

    private fun mapPlatformSummaries(summaries: List<ServicePlatformSummary>): Map<String, Double> {
        return summaries.associate { it.servicePlatform to it.total }
    }

    private fun mapPlatformCountSummaries(summaries: List<ServicePlatformCountSummary>): Map<String, Pair<Double, Int>> {
        return summaries.associate { it.servicePlatform to (it.total to it.count) }
    }
    
    // ========== PAGINATION AND OPTIMIZATION METHODS ==========
    
    // Pagination and optimization constants
    companion object {
        private const val LAZY_LOAD_THRESHOLD = 100
        private const val SUMMARY_CACHE_SIZE = 50
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
        private const val RECENT_DAYS_LIMIT = 30
    }
    
    /**
     * Gets taxi rides with pagination support
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return List of taxi rides for the specified page
     */
    suspend fun getTaxiRidesPaginated(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE): List<TaxiRide> = withContext(Dispatchers.IO) {
        val actualPageSize = pageSize.coerceAtMost(MAX_PAGE_SIZE)
        val offset = page * actualPageSize
        val cacheKey = "paginated_${page}_${actualPageSize}"
        
        paginationCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data
            } else {
                paginationCache.remove(cacheKey)
            }
        }
        
        val rides = taxiRideDao.getTaxiRidesPaginated(actualPageSize, offset)
        paginationCache[cacheKey] = CacheEntry(rides)
        cleanupCache()
        rides
    }
    
    /**
     * Gets taxi rides by date range with pagination
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return List of taxi rides for the specified page and date range
     */
    suspend fun getTaxiRidesByDateRangePaginated(
        startDate: Date, 
        endDate: Date, 
        page: Int, 
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): List<TaxiRide> = withContext(Dispatchers.IO) {
        val actualPageSize = pageSize.coerceAtMost(MAX_PAGE_SIZE)
        val offset = page * actualPageSize
        val cacheKey = "paginated_range_${getDateRangeKey(startDate, endDate)}_${page}_${actualPageSize}"
        
        paginationCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data
            } else {
                paginationCache.remove(cacheKey)
            }
        }
        
        val rides = taxiRideDao.getTaxiRidesByDateRangePaginated(startDate, endDate, actualPageSize, offset)
        paginationCache[cacheKey] = CacheEntry(rides)
        cleanupCache()
        rides
    }
    
    /**
     * Gets total count of taxi rides for pagination calculations
     * @return Total number of taxi rides
     */
    suspend fun getTotalRideCount(): Int = withContext(Dispatchers.IO) {
        val cacheKey = "total_count"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Int
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val count = taxiRideDao.getTotalRideCount()
        summaryCache[cacheKey] = CacheEntry(count)
        count
    }
    
    /**
     * Gets recent taxi rides (last 30 days by default)
     * @param days Number of recent days to fetch
     * @param limit Maximum number of rides to return
     * @return List of recent taxi rides
     */
    suspend fun getRecentTaxiRides(days: Int = RECENT_DAYS_LIMIT, limit: Int = DEFAULT_PAGE_SIZE): List<TaxiRide> = withContext(Dispatchers.IO) {
        val recentDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_${days}_${limit}"
        
        paginationCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data
            } else {
                paginationCache.remove(cacheKey)
            }
        }
        
        val rides = taxiRideDao.getRecentTaxiRides(recentDate, limit)
        paginationCache[cacheKey] = CacheEntry(rides)
        cleanupCache()
        rides
    }
    
    /**
     * Gets recent taxi rides as Flow for real-time updates
     * @param days Number of recent days to observe
     * @return Flow of recent taxi rides
     */
    fun getRecentTaxiRidesFlow(days: Int = RECENT_DAYS_LIMIT): Flow<List<TaxiRide>> {
        val recentDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
        
        return taxiRideDao.getRecentTaxiRidesFlow(recentDate)
    }
    
    /**
     * Gets recent total income with caching
     * @param days Number of recent days to calculate
     * @return Total income for recent period
     */
    suspend fun getRecentTotalIncome(days: Int = RECENT_DAYS_LIMIT): Double = withContext(Dispatchers.IO) {
        val recentDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_income_${days}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Double
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val income = taxiRideDao.getRecentTotalIncome(recentDate) ?: 0.0
        summaryCache[cacheKey] = CacheEntry(income)
        income
    }
    
    /**
     * Gets recent ride count with caching
     * @param days Number of recent days to count
     * @return Number of rides in recent period
     */
    suspend fun getRecentRideCount(days: Int = RECENT_DAYS_LIMIT): Int = withContext(Dispatchers.IO) {
        val recentDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_count_${days}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Int
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val count = taxiRideDao.getRecentRideCount(recentDate)
        summaryCache[cacheKey] = CacheEntry(count)
        count
    }
    
    /**
     * Gets monthly income summary with caching
     * @param limit Number of months to include
     * @return List of monthly summaries
     */
    suspend fun getMonthlyIncomeSummary(limit: Int = 12): List<MonthlySummary> = withContext(Dispatchers.IO) {
        val cacheKey = "monthly_summary_${limit}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cacheEntry.data as List<MonthlySummary>
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val summaries = taxiRideDao.getMonthlyIncomeSummary(limit)
        summaryCache[cacheKey] = CacheEntry(summaries)
        summaries
    }
    
    /**
     * Gets yearly income summary with caching
     * @return List of yearly summaries
     */
    suspend fun getYearlyIncomeSummary(): List<YearlySummary> = withContext(Dispatchers.IO) {
        val cacheKey = "yearly_summary"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cacheEntry.data as List<YearlySummary>
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val summaries = taxiRideDao.getYearlyIncomeSummary()
        summaryCache[cacheKey] = CacheEntry(summaries)
        summaries
    }
    
    /**
     * Invalidates all caches when data changes
     */
    private fun invalidateAllCaches() {
        invalidateDateRangeCache()
        paginationCache.clear()
        summaryCache.clear()
    }

    private suspend fun triggerOnlineBackupIfEnabled() {
        val app = context.applicationContext as GestionTaxiApplication
        if (app.isOnlineBackupEnabled().first()) {
            app.scheduleOnlineBackupDebounced()
        }
    }
    
    /**
     * Checks if the database is large enough to benefit from pagination
     * @return True if pagination should be used
     */
    suspend fun shouldUsePagination(): Boolean = withContext(Dispatchers.IO) {
        getTotalRideCount() > LAZY_LOAD_THRESHOLD
    }
    
    // Método para obtener carreras por rango de fecha

    /**
     * Obtiene el objetivo diario de ingresos
     */
    fun getDailyTarget(): Flow<Double?> {
        return context.dataStore.data.map { preferences ->
            preferences[GestionTaxiApplication.DAILY_TARGET_KEY]?.toDoubleOrNull()
        }
    }

    /**
     * Guarda el objetivo diario de ingresos
     */
    suspend fun setDailyTarget(target: Double) {
        context.dataStore.edit { preferences ->
            preferences[GestionTaxiApplication.DAILY_TARGET_KEY] = target.toString()
        }
    }
}
