package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.MonthlySummary
import com.moham.taxi.data.model.YearlySummary
import com.moham.taxi.data.model.ExpenseTypeSummary
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM expenses
            WHERE date = :date
            AND amount = :amount
            AND type = :type
            AND IFNULL(description, '') = IFNULL(:description, '')
            LIMIT 1
        )
    """)
    suspend fun expenseExists(
        date: Date,
        amount: Double,
        type: ExpenseType,
        description: String?
    ): Boolean

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getExpensesByDateRangeSuspend(startDate: Date, endDate: Date): List<Expense>

    @Query("SELECT * FROM expenses WHERE type = :type ORDER BY date DESC")
    fun getExpensesByType(type: ExpenseType): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesByDateRange(startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesByTypeAndDateRange(type: ExpenseType, startDate: Date, endDate: Date): Double?

    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseCountByDateRange(startDate: Date, endDate: Date): Int?

    @Query("DELETE FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteByDateRange(startDate: Date, endDate: Date)
    
    // Pagination queries
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpensesPaginated(limit: Int, offset: Int): List<Expense>
    
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpensesByDateRangePaginated(startDate: Date, endDate: Date, limit: Int, offset: Int): List<Expense>
    
    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getTotalExpenseCount(): Int
    
    // Optimized queries for recent data
    @Query("SELECT * FROM expenses WHERE date >= :recentDate ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentExpenses(recentDate: Date, limit: Int): List<Expense>
    
    @Query("SELECT * FROM expenses WHERE date >= :recentDate ORDER BY date DESC")
    fun getRecentExpensesFlow(recentDate: Date): Flow<List<Expense>>
    
    // Summary queries for dashboard
    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :recentDate")
    suspend fun getRecentTotalExpenses(recentDate: Date): Double?
    
    @Query("SELECT COUNT(*) FROM expenses WHERE date >= :recentDate")
    suspend fun getRecentExpenseCount(recentDate: Date): Int
    
    // Monthly/yearly aggregations
    @Query("SELECT strftime('%Y-%m', date/1000, 'unixepoch') as month, SUM(amount) as total FROM expenses GROUP BY month ORDER BY month DESC LIMIT :limit")
    suspend fun getMonthlyExpenseSummary(limit: Int): List<MonthlySummary>
    
    @Query("SELECT strftime('%Y', date/1000, 'unixepoch') as year, SUM(amount) as total FROM expenses GROUP BY year ORDER BY year DESC")
    suspend fun getYearlyExpenseSummary(): List<YearlySummary>
    
    // Expense type summaries
    @Query("SELECT type, SUM(amount) as total FROM expenses WHERE date BETWEEN :startDate AND :endDate GROUP BY type")
    suspend fun getExpensesByTypeInDateRange(startDate: Date, endDate: Date): List<ExpenseTypeSummary>
}
