package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StopwatchService
import com.example.data.DriverDatabase
import com.example.data.DriverRecord
import com.example.data.DriverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class PeriodView {
    DAILY, WEEKLY, MONTHLY, ANNUAL
}

data class PeriodSummary(
    val totalRevenue: Double = 0.0,
    val totalHours: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val avgHourlyRate: Double = 0.0,
    val expenseFuel: Double = 0.0,
    val expenseRent: Double = 0.0,
    val expenseMeal: Double = 0.0,
    val expenseMisc: Double = 0.0
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fluxo_driver_prefs", Context.MODE_PRIVATE)
    private val database = DriverDatabase.getDatabase(application)
    private val repository = DriverRepository(database.driverRecordDao)

    // Persistent Settings Flows
    val appThemeMode = MutableStateFlow(prefs.getInt("theme_mode", 0)) // 0 = System, 1 = Light, 2 = Dark
    val weeklyClosureDay = MutableStateFlow(prefs.getInt("weekly_closure_day", Calendar.MONDAY))
    val monthlyRevenueGoal = MutableStateFlow(prefs.getFloat("monthly_revenue_goal", 0f).toDouble())
    val driveFolderName = MutableStateFlow(prefs.getString("drive_folder", "Fluxo Driver") ?: "Fluxo Driver")
    val reportFormat = MutableStateFlow(prefs.getString("report_format", "CSV") ?: "CSV")
    val vehicleType = MutableStateFlow(prefs.getInt("vehicle_type", 0)) // 0 = Combustão, 1 = Híbrido, 2 = Elétrico
    val vehicleConsumption = MutableStateFlow(prefs.getFloat("vehicle_consumption", 10.0f).toDouble())
    val fuelPriceEstimate = MutableStateFlow(prefs.getFloat("fuel_price", 5.50f).toDouble())
    val vehicleOwnership = MutableStateFlow(prefs.getInt("vehicle_ownership", 0)) // 0 = Próprio, 1 = Alugado
    val vehicleRentCost = MutableStateFlow(prefs.getFloat("vehicle_rent", 0f).toDouble())
    val vehicleRentWeeklyCost = MutableStateFlow(prefs.getFloat("vehicle_rent_weekly", 0f).toDouble())
    val vehicleKmAllowance = MutableStateFlow(prefs.getFloat("vehicle_km_allowance", 0f).toDouble())
    val vehicleExtraKmCost = MutableStateFlow(prefs.getFloat("vehicle_extra_km_cost", 0f).toDouble())
    val vehicleOwnCost = MutableStateFlow(prefs.getFloat("vehicle_own_cost", 0f).toDouble())

    // Simulated/Real Google Login User Info
    val googleUserEmail = MutableStateFlow<String?>(prefs.getString("google_email", null))
    val googleUserName = MutableStateFlow<String?>(null)
    val googleUserPhotoUrl = MutableStateFlow<String?>(null)
    val googleIsSyncing = MutableStateFlow(false)

    init {
        // Load additional string prefs
        googleUserEmail.value = prefs.getString("google_email", null)
        googleUserName.value = prefs.getString("google_name", null)
        googleUserPhotoUrl.value = prefs.getString("google_photo", null)
    }

    // Stopwatch logic references
    val stopwatchSeconds = StopwatchState.stopwatchSeconds
    val isStopwatchRunning = StopwatchState.isStopwatchRunning

    val lunchStopwatchSeconds = StopwatchState.lunchStopwatchSeconds
    val isLunchStopwatchRunning = StopwatchState.isLunchStopwatchRunning

    private fun sendServiceAction(action: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, StopwatchService::class.java).apply {
            this.action = action
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startStopwatch() {
        sendServiceAction(StopwatchService.ACTION_START_WORK)
    }

    fun pauseStopwatch() {
        sendServiceAction(StopwatchService.ACTION_PAUSE_WORK)
    }

    fun resetStopwatch() {
        StopwatchState.stopwatchSeconds.value = 0L
        sendServiceAction(StopwatchService.ACTION_PAUSE_WORK)
    }

    fun startLunchStopwatch() {
        sendServiceAction(StopwatchService.ACTION_START_LUNCH)
    }

    fun pauseLunchStopwatch() {
        sendServiceAction(StopwatchService.ACTION_PAUSE_LUNCH)
    }

    fun resetLunchStopwatch() {
        StopwatchState.lunchStopwatchSeconds.value = 0L
        sendServiceAction(StopwatchService.ACTION_PAUSE_LUNCH)
    }

    // Google login update flow
    fun onGoogleSignInSuccess(email: String, displayName: String?, photoUrl: String?) {
        googleUserEmail.value = email
        googleUserName.value = displayName
        googleUserPhotoUrl.value = photoUrl

        prefs.edit().apply {
            putString("google_email", email)
            putString("google_name", displayName)
            putString("google_photo", photoUrl)
            apply()
        }
    }

    fun onGoogleSignOut() {
        googleUserEmail.value = null
        googleUserName.value = null
        googleUserPhotoUrl.value = null

        prefs.edit().apply {
            remove("google_email")
            remove("google_name")
            remove("google_photo")
            apply()
        }
    }

    // Settings Modification Helpers
    fun setAppThemeMode(mode: Int) {
        appThemeMode.value = mode
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun setWeeklyClosureDay(day: Int) {
        weeklyClosureDay.value = day
        prefs.edit().putInt("weekly_closure_day", day).apply()
    }

    fun setMonthlyRevenueGoal(goal: Double) {
        monthlyRevenueGoal.value = goal
        prefs.edit().putFloat("monthly_revenue_goal", goal.toFloat()).apply()
    }

    fun setDriveFolderName(name: String) {
        driveFolderName.value = name
        prefs.edit().putString("drive_folder", name).apply()
    }

    fun setReportFormat(format: String) {
        reportFormat.value = format
        prefs.edit().putString("report_format", format).apply()
    }

    fun setVehicleType(type: Int) {
        vehicleType.value = type
        prefs.edit().putInt("vehicle_type", type).apply()
    }

    fun setVehicleConsumption(consumption: Double) {
        vehicleConsumption.value = consumption
        prefs.edit().putFloat("vehicle_consumption", consumption.toFloat()).apply()
    }

    fun setFuelPriceEstimate(price: Double) {
        fuelPriceEstimate.value = price
        prefs.edit().putFloat("fuel_price", price.toFloat()).apply()
    }

    fun setVehicleOwnership(ownership: Int) {
        vehicleOwnership.value = ownership
        prefs.edit().putInt("vehicle_ownership", ownership).apply()
    }

    fun setVehicleRentCost(cost: Double) {
        vehicleRentCost.value = cost
        prefs.edit().putFloat("vehicle_rent", cost.toFloat()).apply()
    }

    fun setVehicleRentWeeklyCost(cost: Double) {
        vehicleRentWeeklyCost.value = cost
        prefs.edit().putFloat("vehicle_rent_weekly", cost.toFloat()).apply()
    }

    fun setVehicleKmAllowance(allowance: Double) {
        vehicleKmAllowance.value = allowance
        prefs.edit().putFloat("vehicle_km_allowance", allowance.toFloat()).apply()
    }

    fun setVehicleExtraKmCost(cost: Double) {
        vehicleExtraKmCost.value = cost
        prefs.edit().putFloat("vehicle_extra_km_cost", cost.toFloat()).apply()
    }

    fun setVehicleOwnCost(cost: Double) {
        vehicleOwnCost.value = cost
        prefs.edit().putFloat("vehicle_own_cost", cost.toFloat()).apply()
    }

    // DB Records access Flow
    val allRecords: StateFlow<List<DriverRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering logic
    val currentPeriod = MutableStateFlow(PeriodView.DAILY)
    val referenceDateMillis = MutableStateFlow(System.currentTimeMillis())

    // Compute start and end mills for filtering based on current period and reference date
    val filteredRecords: StateFlow<List<DriverRecord>> = combine(
        allRecords,
        currentPeriod,
        referenceDateMillis
    ) { records, period, refMillis ->
        val range = getPeriodRange(period, refMillis)
        records.filter { it.dateMillis in range.first..range.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodSummary: StateFlow<PeriodSummary> = filteredRecords.map { records ->
        if (records.isEmpty()) {
            PeriodSummary()
        } else {
            val totalRev = records.sumOf { it.revenue }
            val totalHrs = records.sumOf { it.hoursWorked }
            val totalKm = records.sumOf { it.kmDriven }
            
            val totalMeal = records.sumOf { it.expenseMeal }
            val totalRent = records.sumOf { it.expenseRent }
            val totalMisc = records.sumOf { it.expenseMisc }
            val totalFuel = records.sumOf { it.expenseFuel }
            
            val totalExp = totalFuel + totalRent + totalMeal + totalMisc
            val netProf = totalRev - totalExp
            val avgRate = if (totalHrs > 0.0) netProf / totalHrs else 0.0
            
            PeriodSummary(
                totalRevenue = totalRev,
                totalHours = totalHrs,
                totalKm = totalKm,
                totalExpenses = totalExp,
                netProfit = netProf,
                avgHourlyRate = avgRate,
                expenseMeal = totalMeal,
                expenseRent = totalRent,
                expenseMisc = totalMisc,
                expenseFuel = totalFuel
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    val monthlyRevenueProgress: StateFlow<Double> = allRecords.map { records ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val thisMonthRecords = records.filter { record ->
            val recCal = Calendar.getInstance().apply { timeInMillis = record.dateMillis }
            recCal.get(Calendar.MONTH) == currentMonth && recCal.get(Calendar.YEAR) == currentYear
        }
        
        thisMonthRecords.sumOf { it.revenue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lastRentValue: StateFlow<Double> = allRecords.map { records ->
        records.firstOrNull { it.expenseRent > 0 }?.expenseRent ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Edit Item State
    private val _editingRecord = MutableStateFlow<DriverRecord?>(null)
    val editingRecord = _editingRecord.asStateFlow()

    fun setEditingRecord(record: DriverRecord?) {
        _editingRecord.value = record
    }

    // CRUD Ops
    fun addRecord(record: DriverRecord) {
        viewModelScope.launch {
            repository.insert(record)
        }
    }

    fun updateRecord(record: DriverRecord) {
        viewModelScope.launch {
            repository.update(record)
        }
    }

    fun deleteRecord(record: DriverRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    // Date calculations helper
    private fun getPeriodRange(period: PeriodView, dateMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        resetTime(calendar)

        return when (period) {
            PeriodView.DAILY -> {
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            PeriodView.WEEKLY -> {
                // Adjust start to matching closure / start of week
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val targetStartDay = weeklyClosureDay.value // e.g., Calendar.MONDAY
                
                val diff = currentDayOfWeek - targetStartDay
                val daysToSubtract = if (diff >= 0) diff else 7 + diff
                calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                val start = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_YEAR, 7)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            PeriodView.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
            PeriodView.ANNUAL -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                val end = calendar.timeInMillis - 1
                Pair(start, end)
            }
        }
    }

    private fun resetTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    fun shiftPeriod(amount: Int) {
        val calendar = Calendar.getInstance().apply { timeInMillis = referenceDateMillis.value }
        when (currentPeriod.value) {
            PeriodView.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            PeriodView.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, amount)
            PeriodView.MONTHLY -> calendar.add(Calendar.MONTH, amount)
            PeriodView.ANNUAL -> calendar.add(Calendar.YEAR, amount)
        }
        referenceDateMillis.value = calendar.timeInMillis
    }
}
