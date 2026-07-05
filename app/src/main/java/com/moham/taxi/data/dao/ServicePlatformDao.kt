package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.ServicePlatform
import kotlinx.coroutines.flow.Flow

@Dao
interface ServicePlatformDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(servicePlatform: ServicePlatform): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(servicePlatform: ServicePlatform)

    @Delete
    suspend fun delete(servicePlatform: ServicePlatform)

    @Query("SELECT * FROM service_platforms ORDER BY name ASC")
    fun getAllServicePlatforms(): Flow<List<ServicePlatform>>

    @Query("SELECT * FROM service_platforms WHERE id = :id")
    suspend fun getServicePlatformById(id: Long): ServicePlatform?

    @Query("SELECT EXISTS(SELECT 1 FROM service_platforms WHERE name = :name LIMIT 1)")
    suspend fun servicePlatformExists(name: String): Boolean

    @Query("SELECT * FROM service_platforms WHERE name = :name LIMIT 1")
    suspend fun getServicePlatformByName(name: String): ServicePlatform?
}
