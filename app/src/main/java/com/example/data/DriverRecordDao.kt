package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverRecordDao {
    @Query("SELECT * FROM driver_records ORDER BY dateMillis DESC, id DESC")
    fun getAllRecordsFlow(): Flow<List<DriverRecord>>

    @Query("SELECT * FROM driver_records WHERE dateMillis >= :startMillis AND dateMillis < :endMillis ORDER BY dateMillis ASC")
    fun getRecordsBetweenDatesFlow(startMillis: Long, endMillis: Long): Flow<List<DriverRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DriverRecord): Long

    @Update
    suspend fun updateRecord(record: DriverRecord)

    @Delete
    suspend fun deleteRecord(record: DriverRecord)

    @Query("DELETE FROM driver_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
