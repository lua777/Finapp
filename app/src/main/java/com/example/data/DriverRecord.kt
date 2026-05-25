package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_records")
data class DriverRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMillis: Long,
    val hoursWorked: Double,
    val kmDriven: Double,
    val revenue: Double,
    val expenseMeal: Double = 0.0,
    val expenseRent: Double = 0.0,
    val expenseFuel: Double = 0.0,
    val expenseMisc: Double = 0.0,
    val description: String = ""
)
