package com.moham.taxi.data.model

/**
 * Data class for monthly income summary
 */
data class MonthlySummary(
    val month: String,
    val total: Double
)

/**
 * Data class for yearly income summary
 */
data class YearlySummary(
    val year: String,
    val total: Double
)

/**
 * Data class for period summary with additional metrics
 */
data class PeriodSummary(
    val period: String,
    val totalIncome: Double,
    val rideCount: Int,
    val averageRide: Double = if (rideCount > 0) totalIncome / rideCount else 0.0
)

/**
 * Data class for expense type summary
 */
data class ExpenseTypeSummary(
    val type: String,
    val total: Double
)