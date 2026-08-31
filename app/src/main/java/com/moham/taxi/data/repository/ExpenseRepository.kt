package com.moham.taxi.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.dao.ExpenseDao
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.MonthlySummary
import com.moham.taxi.data.model.YearlySummary
import com.moham.taxi.data.model.ExpenseTypeSummary
import com.moham.taxi.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class ExpenseRepository(private val expenseDao: ExpenseDao, private val database: AppDatabase, private val context: Context) : BaseRepository() {
    
    // Sistema de caché mejorado para gastos frecuentes
    private val expenseCache = ConcurrentHashMap<Long, CacheEntry<Expense>>()
    private val dateRangeCache = ConcurrentHashMap<String, CacheEntry<List<Expense>>>()
    private val totalCache = ConcurrentHashMap<String, CacheEntry<Double>>()
    private val paginationCache = ConcurrentHashMap<String, CacheEntry<List<Expense>>>()
    private val summaryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    /**
     * Enhanced cache cleanup that includes all cache types
     */
    private fun cleanupCache() {
        cleanupCacheMap(expenseCache)
        cleanupCacheMap(dateRangeCache)
        cleanupCacheMap(totalCache)
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
     * Invalida todo el caché relacionado con rangos de fechas y totales
     */
    private fun invalidateDateRangeCache() {
        dateRangeCache.clear()
        totalCache.clear()
    }
    
    // Clase para manejar rangos de fechas
    private data class DateRange(
        val start: Date,
        val end: Date
    )
    
    // Método de utilidad para rangos de mes usando DateUtils
    private fun getMonthRange(date: Date = Date()): DateRange {
        val (start, end) = DateUtils.getMonthRange(date)
        return DateRange(start, end)
    }

    private fun getYearRange(date: Date = Date()): DateRange {
        val (start, end) = DateUtils.getYearRange(date)
        return DateRange(start, end)
    }
    
    private suspend fun getWeekRange(date: Date = Date()): DateRange {
        val context = getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeek = application.getFirstDayOfWeek().first()
        
        return com.moham.taxi.utils.DateUtils.getWeekRange(date, firstDayOfWeek).let { (start, end) ->
            DateRange(start, end)
        }
    }
    
    fun getContext(): Context {
        return context
    }
    
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    
    suspend fun insert(expense: Expense, triggerOnlineBackup: Boolean = true): Long = withContext(Dispatchers.IO) {
        val id = expenseDao.insert(expense)
        expenseCache[id] = CacheEntry(expense)
        invalidateDateRangeCache()
        totalCache.clear() // Invalidar caché de totales
        cleanupCache()
        val application = context.applicationContext as GestionTaxiApplication
        application.savePendingBackup(true)
        application.scheduleFirebaseSyncDebounced()
        application.triggerImmediateFirebaseSync()
        id
    }
    
    suspend fun update(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.update(expense)
        expenseCache[expense.id] = CacheEntry(expense)
        invalidateDateRangeCache()
        totalCache.clear() // Invalidar caché de totales
        cleanupCache()
        val application = context.applicationContext as GestionTaxiApplication
        application.savePendingBackup(true)
        application.scheduleFirebaseSyncDebounced()
        application.triggerImmediateFirebaseSync()
    }
    
    suspend fun delete(expense: Expense) = withContext(Dispatchers.IO) {
        val application = context.applicationContext as GestionTaxiApplication
        if (!expense.firestoreId.isNullOrEmpty()) {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("expenses")
                    .document(expense.firestoreId)
                    .delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        expenseDao.delete(expense)
        expenseCache.remove(expense.id)
        invalidateDateRangeCache()
        totalCache.clear() // Invalidar caché de totales
        cleanupCache()
        application.savePendingBackup(true)
        application.triggerImmediateFirebaseSync()
    }
    
    suspend fun insertMultiple(expenses: List<Expense>): List<Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            expenses.chunked(BATCH_SIZE).flatMap { batch ->
                batch.map { expense ->
                    val id = expenseDao.insert(expense)
                    expenseCache[id] = CacheEntry(expense)
                    id
                }
            }
        }.also {
            invalidateDateRangeCache()
            totalCache.clear() // Invalidar caché de totales
            cleanupCache()
        }
    }
    
    suspend fun getExpenseById(id: Long): Expense? {
        expenseCache[id]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return cacheEntry.data
            } else {
                expenseCache.remove(id)
            }
        }
        return expenseDao.getExpenseById(id)?.also { expense ->
            expenseCache[id] = CacheEntry(expense)
        }
    }
    
    private fun getDateRangeKey(startDate: Date, endDate: Date): String {
        return "${startDate.time}_${endDate.time}"
    }
    
    fun getTodayExpenses(): Flow<List<Expense>> {
        val dayRange = DateUtils.getCurrentDayRange()
        val range = DateRange(dayRange.first, dayRange.second)
        return expenseDao.getExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getTodayExpenseCount(): Int {
        val dayRange = DateUtils.getCurrentDayRange()
        val range = DateRange(dayRange.first, dayRange.second)
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    fun getMonthExpenses(): Flow<List<Expense>> {
        val range = getMonthRange()
        return expenseDao.getExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getTodayExpensesTotal(): Double {
        val dayRange = DateUtils.getCurrentDayRange()
        val range = DateRange(dayRange.first, dayRange.second)
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getMonthExpensesTotal(): Double {
        val range = getMonthRange()
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getTodayFuelExpenses(): Double {
        val dayRange = DateUtils.getCurrentDayRange()
        val range = DateRange(dayRange.first, dayRange.second)
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun getMonthFuelExpenses(): Double {
        val range = getMonthRange()
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun getMonthExpenseCount(): Int {
        val range = getMonthRange()
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    fun getExpensesForDate(date: Date): Flow<List<Expense>> {
        val dayRange = DateUtils.getDayRange(date)
        val range = DateRange(dayRange.first, dayRange.second)
        return expenseDao.getExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getExpensesTotalForDate(date: Date): Double {
        val dayRange = DateUtils.getDayRange(date)
        val range = DateRange(dayRange.first, dayRange.second)
        println("DEBUG REPO: Consultando gastos desde ${range.start.time} hasta ${range.end.time}")
        val result = getTotalExpensesByDateRange(range.start, range.end)
        println("DEBUG REPO: Total de gastos encontrado: $result")
        return result
    }
    
    suspend fun getFuelExpensesForDate(date: Date): Double {
        val dayRange = DateUtils.getDayRange(date)
        val range = DateRange(dayRange.first, dayRange.second)
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun getExpenseCountForDate(date: Date): Int {
        val dayRange = DateUtils.getDayRange(date)
        val range = DateRange(dayRange.first, dayRange.second)
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate)
    }
    
    suspend fun getExpensesByDateRangeSuspend(startDate: Date, endDate: Date): List<Expense> {
        return expenseDao.getExpensesByDateRangeSuspend(startDate, endDate)
    }

    fun getExpensesByType(type: ExpenseType): Flow<List<Expense>> {
        return expenseDao.getExpensesByType(type)
    }
    
    suspend fun getTotalExpensesForDate(date: Date): Double {
        val dayRange = DateUtils.getDayRange(date)
        val range = DateRange(dayRange.first, dayRange.second)
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getTotalExpensesByDateRange(startDate: Date, endDate: Date): Double {
        val cacheKey = getDateRangeKey(startDate, endDate)
        totalCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return cacheEntry.data
            } else {
                totalCache.remove(cacheKey)
            }
        }
        
        return expenseDao.getTotalExpensesByDateRange(startDate, endDate)?.also { total ->
            totalCache[cacheKey] = CacheEntry(total)
            cleanupCache()
        } ?: 0.0
    }
    
    private suspend fun getTotalExpensesByTypeAndDateRange(type: ExpenseType, startDate: Date, endDate: Date): Double {
        val cacheKey = "${type}_${getDateRangeKey(startDate, endDate)}"
        totalCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return cacheEntry.data
            } else {
                totalCache.remove(cacheKey)
            }
        }
        
        return expenseDao.getTotalExpensesByTypeAndDateRange(type, startDate, endDate)?.also { total ->
            totalCache[cacheKey] = CacheEntry(total)
            cleanupCache()
        } ?: 0.0
    }
    
    suspend fun getCurrentWeekExpenses(): Double {
        val range = getWeekRange()
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getCurrentWeekExpenseCount(): Int {
        val range = getWeekRange()
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    suspend fun getCurrentWeekFuelExpenses(): Double {
        val range = getWeekRange()
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun getWeekExpensesForDate(date: Date): Double {
        val range = getWeekRange(date)
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getWeekExpenseCountForDate(date: Date): Int {
        val range = getWeekRange(date)
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    suspend fun getWeekFuelExpensesForDate(date: Date): Double {
        val range = getWeekRange(date)
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun getMonthExpensesForDate(date: Date): Double {
        val range = getMonthRange(date)
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getMonthExpenseCountForDate(date: Date): Int {
        val range = getMonthRange(date)
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    suspend fun getMonthFuelExpensesForDate(date: Date): Double {
        val range = getMonthRange(date)
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }

    suspend fun getYearExpensesForDate(date: Date): Double {
        val range = getYearRange(date)
        return getTotalExpensesByDateRange(range.start, range.end)
    }
    
    suspend fun getYearExpenseCountForDate(date: Date): Int {
        val range = getYearRange(date)
        return expenseDao.getExpenseCountByDateRange(range.start, range.end) ?: 0
    }
    
    suspend fun getYearFuelExpensesForDate(date: Date): Double {
        val range = getYearRange(date)
        return getTotalExpensesByTypeAndDateRange(ExpenseType.FUEL, range.start, range.end)
    }
    
    suspend fun insertExpense(expense: Expense): Long {
        return insert(expense, triggerOnlineBackup = false)
    }
    
    suspend fun deleteExpensesByDateRange(startDate: Date, endDate: Date) {
        expenseDao.deleteByDateRange(startDate, endDate)
        invalidateDateRangeCache()
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
     * Gets expenses with pagination support
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return List of expenses for the specified page
     */
    suspend fun getExpensesPaginated(page: Int, pageSize: Int = DEFAULT_PAGE_SIZE): List<Expense> = withContext(Dispatchers.IO) {
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
        
        val expenses = expenseDao.getExpensesPaginated(actualPageSize, offset)
        paginationCache[cacheKey] = CacheEntry(expenses)
        cleanupCache()
        expenses
    }
    
    /**
     * Gets expenses by date range with pagination
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return List of expenses for the specified page and date range
     */
    suspend fun getExpensesByDateRangePaginated(
        startDate: Date, 
        endDate: Date, 
        page: Int, 
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): List<Expense> = withContext(Dispatchers.IO) {
        val actualPageSize = pageSize.coerceAtMost(MAX_PAGE_SIZE)
        val offset = page * actualPageSize
        val cacheKey = "paginated_range_${startDate.time}_${endDate.time}_${page}_${actualPageSize}"
        
        paginationCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data
            } else {
                paginationCache.remove(cacheKey)
            }
        }
        
        val expenses = expenseDao.getExpensesByDateRangePaginated(startDate, endDate, actualPageSize, offset)
        paginationCache[cacheKey] = CacheEntry(expenses)
        cleanupCache()
        expenses
    }
    
    /**
     * Gets total count of expenses for pagination calculations
     * @return Total number of expenses
     */
    suspend fun getTotalExpenseCount(): Int = withContext(Dispatchers.IO) {
        val cacheKey = "total_count"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Int
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val count = expenseDao.getTotalExpenseCount()
        summaryCache[cacheKey] = CacheEntry(count)
        count
    }
    
    /**
     * Gets recent expenses (last 30 days by default)
     * @param days Number of recent days to fetch
     * @param limit Maximum number of expenses to return
     * @return List of recent expenses
     */
    suspend fun getRecentExpenses(days: Int = RECENT_DAYS_LIMIT, limit: Int = DEFAULT_PAGE_SIZE): List<Expense> = withContext(Dispatchers.IO) {
        val recentDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_${days}_${limit}"
        
        paginationCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data
            } else {
                paginationCache.remove(cacheKey)
            }
        }
        
        val expenses = expenseDao.getRecentExpenses(recentDate, limit)
        paginationCache[cacheKey] = CacheEntry(expenses)
        cleanupCache()
        expenses
    }
    
    /**
     * Gets recent expenses as Flow for real-time updates
     * @param days Number of recent days to observe
     * @return Flow of recent expenses
     */
    fun getRecentExpensesFlow(days: Int = RECENT_DAYS_LIMIT): Flow<List<Expense>> {
        val recentDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.time
        
        return expenseDao.getRecentExpensesFlow(recentDate)
    }
    
    /**
     * Gets recent total expenses with caching
     * @param days Number of recent days to calculate
     * @return Total expenses for recent period
     */
    suspend fun getRecentTotalExpenses(days: Int = RECENT_DAYS_LIMIT): Double = withContext(Dispatchers.IO) {
        val recentDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_total_${days}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Double
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val total = expenseDao.getRecentTotalExpenses(recentDate) ?: 0.0
        summaryCache[cacheKey] = CacheEntry(total)
        total
    }
    
    /**
     * Gets recent expense count with caching
     * @param days Number of recent days to count
     * @return Number of expenses in recent period
     */
    suspend fun getRecentExpenseCount(days: Int = RECENT_DAYS_LIMIT): Int = withContext(Dispatchers.IO) {
        val recentDate = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -days)
        }.time
        
        val cacheKey = "recent_count_${days}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return@withContext cacheEntry.data as Int
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val count = expenseDao.getRecentExpenseCount(recentDate)
        summaryCache[cacheKey] = CacheEntry(count)
        count
    }
    
    /**
     * Gets monthly expense summary with caching
     * @param limit Number of months to include
     * @return List of monthly summaries
     */
    suspend fun getMonthlyExpenseSummary(limit: Int = 12): List<MonthlySummary> = withContext(Dispatchers.IO) {
        val cacheKey = "monthly_summary_${limit}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cacheEntry.data as List<MonthlySummary>
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val summaries = expenseDao.getMonthlyExpenseSummary(limit)
        summaryCache[cacheKey] = CacheEntry(summaries)
        summaries
    }
    
    /**
     * Gets yearly expense summary with caching
     * @return List of yearly summaries
     */
    suspend fun getYearlyExpenseSummary(): List<YearlySummary> = withContext(Dispatchers.IO) {
        val cacheKey = "yearly_summary"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cacheEntry.data as List<YearlySummary>
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val summaries = expenseDao.getYearlyExpenseSummary()
        summaryCache[cacheKey] = CacheEntry(summaries)
        summaries
    }
    
    /**
     * Gets expenses by type in date range with caching
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of expense type summaries
     */
    suspend fun getExpensesByTypeInDateRange(startDate: Date, endDate: Date): List<ExpenseTypeSummary> = withContext(Dispatchers.IO) {
        val cacheKey = "type_summary_${startDate.time}_${endDate.time}"
        
        summaryCache[cacheKey]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                @Suppress("UNCHECKED_CAST")
                return@withContext cacheEntry.data as List<ExpenseTypeSummary>
            } else {
                summaryCache.remove(cacheKey)
            }
        }
        
        val summaries = expenseDao.getExpensesByTypeInDateRange(startDate, endDate)
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
        getTotalExpenseCount() > LAZY_LOAD_THRESHOLD
    }
}
