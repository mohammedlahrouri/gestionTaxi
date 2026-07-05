package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moham.taxi.data.model.ServiceMode.BOTH

@Entity(tableName = "service_platforms")
data class ServicePlatform(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val commissionPercentage: Double? = null,
    val commissionVat: Double? = null,
    val useAlternativeMath: Boolean = false,
    val serviceMode: ServiceMode = BOTH
)

enum class ServiceMode {
    BOTH,
    METER_ONLY,
    FIXED_ONLY
}

data class ServicePlatformSummary(
    val servicePlatform: String,
    val total: Double
)

data class ServicePlatformCountSummary(
    val servicePlatform: String,
    val total: Double,
    val count: Int
)

data class IncomeCountSummary(
    val total: Double,
    val count: Int
)
