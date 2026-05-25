package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverRecordDao {
    @Query("SELECT * FROM driver_records ORDER BY dateMillis DESC")
    fun getAllRecordsFlow(): Flow<List<DriverRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DriverRecord)

    @Update
    suspend fun updateRecord(record: DriverRecord)

    @Delete
    suspend fun deleteRecord(record: DriverRecord)
}
