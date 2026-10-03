package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletions")
data class PendingDeletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreId: String,
    val collectionName: String, // "rides" or "expenses"
    val createdAt: Long = System.currentTimeMillis()
)
