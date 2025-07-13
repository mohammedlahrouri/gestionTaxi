package com.moham.taxi.data.model

/**
 * Clase de datos para representar el resumen de pagos por método
 */
data class PaymentSummary(
    val paymentMethod: String,
    val total: Double
) 
