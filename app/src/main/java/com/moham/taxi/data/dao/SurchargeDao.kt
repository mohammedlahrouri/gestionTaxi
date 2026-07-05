package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.Surcharge
import kotlinx.coroutines.flow.Flow

@Dao
interface SurchargeDao {
    @Query("SELECT * FROM surcharges ORDER BY id ASC")
    fun getAllSurcharges(): Flow<List<Surcharge>>

    @Query("SELECT * FROM surcharges WHERE id = :id")
    suspend fun getSurchargeById(id: Long): Surcharge?

    @Query("SELECT COUNT(*) FROM surcharges")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(surcharge: Surcharge)

    @Update
    suspend fun update(surcharge: Surcharge)

    @Delete
    suspend fun delete(surcharge: Surcharge)

    @Query("DELETE FROM surcharges")
    suspend fun deleteAll()
}
