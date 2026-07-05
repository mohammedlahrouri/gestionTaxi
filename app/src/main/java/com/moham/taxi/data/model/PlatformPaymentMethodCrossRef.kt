package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "platform_payment_methods",
    primaryKeys = ["platformId", "paymentMethodId"],
    foreignKeys = [
        ForeignKey(
            entity = ServicePlatform::class,
            parentColumns = ["id"],
            childColumns = ["platformId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PaymentMethod::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["platformId"]),
        Index(value = ["paymentMethodId"])
    ]
)
data class PlatformPaymentMethodCrossRef(
    val platformId: Long,
    val paymentMethodId: Long
)

