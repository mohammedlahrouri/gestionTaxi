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
        Index(value = ["date", "price"]), // Compound index for date range + price queries
        Index(value = ["servicePlatform"]),
        Index(value = ["workingDate"]),
        Index(value = ["paymentMethodId"]),
        Index(value = ["servicePlatformId"]),
        Index(value = ["tariffId"])
    ]
)
data class TaxiRide(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val origin: String,
    val destination: String,
    val price: Double,
    val tip: Double? = null,
    val netPrice: Double? = null,
    val commissionPercentAtTime: Double? = null,
    val commissionVatAtTime: Double? = null,
    val paymentMethod: String,
    val date: Date = Date(),
    val rideTime: String = "00:00",
    val serviceType: String? = null,
    val servicePlatform: String? = "Directo",
    val workingDate: Date? = null,
    val paymentMethodId: Long? = null,
    val servicePlatformId: Long? = null,
    val tariffId: Long? = null,
    val rideDurationMinutes: Int? = null,
    val ticketPhotoPath: String? = null,
    val realDate: Date = Date()
) {
    companion object {
        const val SERVICE_TYPE_METER = "METER"
        const val SERVICE_TYPE_FIXED = "FIXED"
    }

    /**
     * Valida que los datos de la carrera sean correctos
     * @return true si los datos son válidos, false en caso contrario
     */
    fun isValid(): Boolean {
        return price > 0 && 
               paymentMethod.isNotBlank() &&
               date.time <= System.currentTimeMillis() // La fecha no puede ser futura
    }
}
