package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.Tariff
import kotlinx.coroutines.flow.Flow

@Dao
interface TariffDao {
    @Query("SELECT * FROM tariffs ORDER BY id ASC")
    fun getAllTariffs(): Flow<List<Tariff>>

    @Query("SELECT * FROM tariffs WHERE id = :id")
    suspend fun getTariffById(id: Long): Tariff?

    @Query("SELECT COUNT(*) FROM tariffs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tariff: Tariff)

    @Update
    suspend fun update(tariff: Tariff)

    @Delete
    suspend fun delete(tariff: Tariff)

    @Query("DELETE FROM tariffs")
    suspend fun deleteAll()
}
