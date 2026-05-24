package com.example.data

import kotlinx.coroutines.flow.Flow

class DriverRepository(private val dao: DriverRecordDao) {

    val allRecords: Flow<List<DriverRecord>> = dao.getAllRecordsFlow()

    fun getRecordsBetween(start: Long, end: Long): Flow<List<DriverRecord>> {
        return dao.getRecordsBetweenDatesFlow(start, end)
    }

    suspend fun insert(record: DriverRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun update(record: DriverRecord) {
        dao.updateRecord(record)
    }

    suspend fun delete(record: DriverRecord) {
        dao.deleteRecord(record)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteRecordById(id)
    }
}
