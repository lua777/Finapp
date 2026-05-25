package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "driver_records")
data class DriverRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long, // timestamp for the entry
    val hoursWorked: Double, // Hours worked (e.g. 8.5)
    val kmDriven: Double, // Kilometers driven (e.g. 120.4)
    val revenue: Double, // Gross earnings (valor faturado)
    val expenseMeal: Double, // Meal expenses (alimentação)
    val expenseRent: Double, // Rent expenses (aluguel)
    val expenseMisc: Double, // Miscellaneous expenses (miscelâneas)
    val expenseFuel: Double = 0.0, // Fuel/Recharge expenses (abastecimento/recarga)
    val description: String = "" // Optional notes
) {
    // Derived properties
    val totalExpenses: Double
        get() = expenseMeal + expenseRent + expenseMisc + expenseFuel

    val netProfit: Double
        get() = revenue - totalExpenses

    // Earnings indicators
    val grossHourlyRate: Double
        get() = if (hoursWorked > 0) revenue / hoursWorked else 0.0

    val netHourlyRate: Double
        get() = if (hoursWorked > 0) netProfit / hoursWorked else 0.0

    val netMinutelyRate: Double
        get() = if (hoursWorked > 0) netProfit / (hoursWorked * 60) else 0.0

    val grossRevenuePerKm: Double
        get() = if (kmDriven > 0) revenue / kmDriven else 0.0

    val costPerKm: Double
        get() = if (kmDriven > 0) totalExpenses / kmDriven else 0.0

    val profitPerKm: Double
        get() = if (kmDriven > 0) netProfit / kmDriven else 0.0

    // Helper to format date
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }
}
