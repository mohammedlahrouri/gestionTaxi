package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surcharges")
data class Surcharge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val isPerItem: Boolean // false = solo 1 (checkbox), true = múltiple (contador de bultos)
)
