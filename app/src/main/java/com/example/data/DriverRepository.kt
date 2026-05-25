package com.example.data

import kotlinx.coroutines.flow.Flow

class DriverRepository(private val dao: DriverRecordDao) {
    val allRecords: Flow<List<DriverRecord>> = dao.getAllRecordsFlow()

    suspend fun insert(record: DriverRecord) {
        dao.insertRecord(record)
    }

    suspend fun update(record: DriverRecord) {
        dao.updateRecord(record)
    }

    suspend fun delete(record: DriverRecord) {
        dao.deleteRecord(record)
    }
}
