package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ride_surcharge_cross_ref",
    primaryKeys = ["rideId", "surchargeId"],
    foreignKeys = [
        ForeignKey(
            entity = TaxiRide::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Surcharge::class,
            parentColumns = ["id"],
            childColumns = ["surchargeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rideId"]),
        Index(value = ["surchargeId"])
    ]
)
data class RideSurchargeCrossRef(
    val rideId: Long,
    val surchargeId: Long,
    val count: Int = 1
)
