package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.PaymentSummary
import com.moham.taxi.data.model.ServicePlatformSummary
import com.moham.taxi.data.model.ServicePlatformCountSummary
import com.moham.taxi.data.model.IncomeCountSummary
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.MonthlySummary
import com.moham.taxi.data.model.YearlySummary
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaxiRideDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(taxiRide: TaxiRide): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(taxiRide: TaxiRide)

    @Delete
    suspend fun delete(taxiRide: TaxiRide)

    @Query("SELECT * FROM taxi_rides ORDER BY date DESC")
    fun getAllTaxiRides(): Flow<List<TaxiRide>>

    @Query("SELECT * FROM taxi_rides WHERE id = :id")
    suspend fun getTaxiRideById(id: Long): TaxiRide?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM taxi_rides
            WHERE date = :date
            AND price = :price
            AND IFNULL(tip, 0) = IFNULL(:tip, 0)
            AND paymentMethod = :paymentMethod
            AND origin = :origin
            AND destination = :destination
            AND IFNULL(serviceType, '') = IFNULL(:serviceType, '')
            AND IFNULL(servicePlatform, '') = IFNULL(:servicePlatform, '')
            AND rideTime = :rideTime
            LIMIT 1
        )
    """)
    suspend fun taxiRideExists(
        date: Date,
        price: Double,
        tip: Double?,
        paymentMethod: String,
        origin: String,
        destination: String,
        serviceType: String?,
        servicePlatform: String?,
        rideTime: String
    ): Boolean

    @Query("SELECT * FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTaxiRidesByDateRange(startDate: Date, endDate: Date): Flow<List<TaxiRide>>

    @Query("SELECT * FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTaxiRidesByDateRangeSuspend(startDate: Date, endDate: Date): List<TaxiRide>

    @Query("SELECT SUM(COALESCE(netPrice, price)) FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeByDateRange(startDate: Date, endDate: Date): Double?

    @Query("SELECT paymentMethod, SUM(price) as total FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate GROUP BY paymentMethod")
    suspend fun getTotalByPaymentMethod(startDate: Date, endDate: Date): List<PaymentSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(price) as total
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND paymentMethod = 'Via App'
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getAppIncomeByPlatform(startDate: Date, endDate: Date): List<ServicePlatformSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(COALESCE(netPrice, price)) as total
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND paymentMethod = 'Via App'
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getAppNetIncomeByPlatform(startDate: Date, endDate: Date): List<ServicePlatformSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(price) as total
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getTotalIncomeByPlatform(startDate: Date, endDate: Date): List<ServicePlatformSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(COALESCE(netPrice, price)) as total
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getNetIncomeByPlatform(startDate: Date, endDate: Date): List<ServicePlatformSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(price) as total, COUNT(*) as count
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND paymentMethod = 'Via App'
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getAppIncomeByPlatformWithCount(startDate: Date, endDate: Date): List<ServicePlatformCountSummary>

    @Query("""
        SELECT servicePlatform as servicePlatform, SUM(price) as total, COUNT(*) as count
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND servicePlatform IS NOT NULL
        GROUP BY servicePlatform
    """)
    suspend fun getIncomeByPlatformWithCount(startDate: Date, endDate: Date): List<ServicePlatformCountSummary>

    @Query("""
        SELECT COALESCE(SUM(price), 0.0) as total, COUNT(*) as count
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
        AND servicePlatform IS NULL
    """)
    suspend fun getIncomeWithoutPlatform(startDate: Date, endDate: Date): IncomeCountSummary

    @Query("SELECT COUNT(*) FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRideCountByDateRange(startDate: Date, endDate: Date): Int?

    @Query("DELETE FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteByDateRange(startDate: Date, endDate: Date)
    
    // Pagination queries
    @Query("SELECT * FROM taxi_rides ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTaxiRidesPaginated(limit: Int, offset: Int): List<TaxiRide>
    
    @Query("SELECT * FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTaxiRidesByDateRangePaginated(startDate: Date, endDate: Date, limit: Int, offset: Int): List<TaxiRide>
    
    @Query("SELECT COUNT(*) FROM taxi_rides")
    suspend fun getTotalRideCount(): Int
    
    // Optimized queries for recent data
    @Query("SELECT * FROM taxi_rides WHERE date >= :recentDate ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentTaxiRides(recentDate: Date, limit: Int): List<TaxiRide>
    
    @Query("SELECT * FROM taxi_rides WHERE date >= :recentDate ORDER BY date DESC")
    fun getRecentTaxiRidesFlow(recentDate: Date): Flow<List<TaxiRide>>
    
    // Summary queries for dashboard
    @Query("SELECT SUM(price) FROM taxi_rides WHERE date >= :recentDate")
    suspend fun getRecentTotalIncome(recentDate: Date): Double?
    
    @Query("SELECT COUNT(*) FROM taxi_rides WHERE date >= :recentDate")
    suspend fun getRecentRideCount(recentDate: Date): Int
    
    // Monthly/yearly aggregations
    @Query("SELECT strftime('%Y-%m', date/1000, 'unixepoch') as month, SUM(price) as total FROM taxi_rides GROUP BY month ORDER BY month DESC LIMIT :limit")
    suspend fun getMonthlyIncomeSummary(limit: Int): List<MonthlySummary>
    
    @Query("SELECT strftime('%Y', date/1000, 'unixepoch') as year, SUM(price) as total FROM taxi_rides GROUP BY year ORDER BY year DESC")
    suspend fun getYearlyIncomeSummary(): List<YearlySummary>

    @Query("SELECT SUM(COALESCE(tip, 0)) FROM taxi_rides WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalTipsByDateRange(startDate: Date, endDate: Date): Double?

    @Query(
        """
        SELECT
            CASE
                WHEN lower(paymentMethod) = 'via app' THEN COALESCE(servicePlatform, 'Directo')
                ELSE paymentMethod
            END as paymentMethod,
            SUM(COALESCE(tip, 0)) as total
        FROM taxi_rides
        WHERE date BETWEEN :startDate AND :endDate
          AND COALESCE(tip, 0) > 0
        GROUP BY
            CASE
                WHEN lower(paymentMethod) = 'via app' THEN COALESCE(servicePlatform, 'Directo')
                ELSE paymentMethod
            END
        """
    )
    suspend fun getTipsByMethod(startDate: Date, endDate: Date): List<PaymentSummary>
}
