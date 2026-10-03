package com.moham.taxi.data.dao

import androidx.room.*
import com.moham.taxi.data.model.PendingDeletion
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDeletion: PendingDeletion): Long

    @Query("SELECT * FROM pending_deletions ORDER BY createdAt ASC")
    suspend fun getAllPendingDeletions(): List<PendingDeletion>

    @Query("SELECT EXISTS(SELECT 1 FROM pending_deletions WHERE firestoreId = :firestoreId LIMIT 1)")
    suspend fun isPendingDeletion(firestoreId: String): Boolean

    @Delete
    suspend fun delete(pendingDeletion: PendingDeletion)

    @Query("DELETE FROM pending_deletions WHERE firestoreId = :firestoreId")
    suspend fun deleteByFirestoreId(firestoreId: String)

    @Query("SELECT COUNT(*) FROM pending_deletions")
    suspend fun getPendingDeletionsCount(): Int

    @Query("SELECT COUNT(*) FROM pending_deletions")
    fun getPendingDeletionsCountFlow(): Flow<Int>
}
