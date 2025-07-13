package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa una carrera de taxi
 */
@Entity(
    tableName = "taxi_rides",
    indices = [
        Index(value = ["date"]),
        Index(value = ["paymentMethod"]),
        Index(value = ["date", "paymentMethod"]), // Compound index for date range + payment method queries
        Index(value = ["price"]), // Index for price-based queries
        Index(value = ["date", "price"]) // Compound index for date range + price queries
    ]
)
data class TaxiRide(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val origin: String,
    val destination: String,
    val price: Double,
    val paymentMethod: String,
    val date: Date = Date()
) {
    /**
     * Valida que los datos de la carrera sean correctos
     * @return true si los datos son válidos, false en caso contrario
     */
    fun isValid(): Boolean {
        return price > 0 && 
               origin.isNotBlank() &&
               destination.isNotBlank() &&
               paymentMethod.isNotBlank() &&
               date.time <= System.currentTimeMillis() // La fecha no puede ser futura
    }
}
