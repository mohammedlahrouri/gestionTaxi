package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tariffs")
data class Tariff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isFixed: Boolean,
    val fixedPrice: Double? = null,
    val baseFare: Double? = null,
    val pricePerKm: Double? = null,
    val surcharge: Double? = null
)
