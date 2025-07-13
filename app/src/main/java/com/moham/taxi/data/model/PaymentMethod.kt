package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un método de pago
 */
@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
) 
