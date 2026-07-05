package com.moham.taxi.data.model

data class QuoteItem(
    val description: String,
    val quantity: Double?,
    val unitPrice: Double,
    val total: Double
)

data class QuoteData(
    val title: String,
    val items: List<QuoteItem>,
    val totalAmount: Double
)