package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DriverDatabase
import com.example.data.DriverRecord
import com.example.data.DriverRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodView {
    DAILY, WEEKLY, MONTHLY
}

data class PeriodSummary(
    val selectedPeriodType: PeriodView,
    val periodLabel: String,
    val totalRevenue: Double = 0.0,
    val totalHours: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalMeal: Double = 0.0,
    val totalRent: Double = 0.0,
    val totalMisc: Double = 0.0,
    val totalFuel: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val grossHourlyRate: Double = 0.0,
    val netHourlyRate: Double = 0.0,
    val netMinutelyRate: Double = 0.0,
    val grossRevenuePerKm: Double = 0.0,
    val costPerKm: Double = 0.0,
    val profitPerKm: Double = 0.0
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DriverRepository
    private val prefs = application.getSharedPreferences("driver_prefs", android.content.Context.MODE_PRIVATE)

    // Persistent settings states
    val appThemeMode = MutableStateFlow(0) // 0 = System, 1 = Light, 2 = Dark
    val weeklyClosureDay = MutableStateFlow(Calendar.MONDAY) // Calendar.MONDAY = 2, Calendar.SUNDAY = 1 etc
    val monthlyRevenueGoal = MutableStateFlow(0.0) // 0.0 means no active goal
    val driveFolderName = MutableStateFlow("Fluxo Driver")
    val reportFormat = MutableStateFlow("CSV") // CSV or Text
    val vehicleType = MutableStateFlow(0) // 0 = Combustão, 1 = Híbrido, 2 = Elétrico
    val vehicleConsumption = MutableStateFlow(10.0) // Km/L or Km/kWh
    val fuelPriceEstimate = MutableStateFlow(5.50) // Average cost per L or kWh
    val vehicleOwnership = MutableStateFlow(0) // 0 = Próprio, 1 = Alugado
    val vehicleRentCost = MutableStateFlow(0.0) // Cost of rental per day
    val vehicleRentWeeklyCost = MutableStateFlow(0.0) // Cost of weekly rental
    val vehicleKmAllowance = MutableStateFlow(0.0) // KM allowance per week
    val vehicleExtraKmCost = MutableStateFlow(0.0) // Cost per extra KM
    val vehicleOwnCost = MutableStateFlow(0.0) // Cost of maintaining / finance / wear of own vehicle per day

    // Google Integration Stats
    val googleUserEmail = MutableStateFlow<String?>(null)
    val googleUserName = MutableStateFlow<String?>(null)
    val googleUserPhotoUrl = MutableStateFlow<String?>(null)
    val googleIsSyncing = MutableStateFlow(false)

    init {
        val database = DriverDatabase.getDatabase(application)
        repository = DriverRepository(database.dao)
        
        // Load persistent settings
        appThemeMode.value = prefs.getInt("app_theme_mode", 0)
        weeklyClosureDay.value = prefs.getInt("weekly_closure_day", Calendar.MONDAY)
        monthlyRevenueGoal.value = prefs.getFloat("monthly_revenue_goal", 0f).toDouble()
        driveFolderName.value = prefs.getString("drive_folder_name", "Fluxo Driver") ?: "Fluxo Driver"
        reportFormat.value = prefs.getString("report_format", "CSV") ?: "CSV"
        vehicleType.value = prefs.getInt("vehicle_type", 0)
        vehicleConsumption.value = prefs.getFloat("vehicle_consumption", 10f).toDouble()
        fuelPriceEstimate.value = prefs.getFloat("fuel_price_estimate", 5.50f).toDouble()
        vehicleOwnership.value = prefs.getInt("vehicle_ownership", 0)
        vehicleRentCost.value = prefs.getFloat("vehicle_rent_cost", 0f).toDouble()
        vehicleRentWeeklyCost.value = prefs.getFloat("vehicle_rent_weekly_cost", 0f).toDouble()
        vehicleKmAllowance.value = prefs.getFloat("vehicle_km_allowance", 0f).toDouble()
        vehicleExtraKmCost.value = prefs.getFloat("vehicle_extra_km_cost", 0f).toDouble()
        vehicleOwnCost.value = prefs.getFloat("vehicle_own_cost", 0f).toDouble()

        googleUserEmail.value = prefs.getString("google_user_email", null)
        googleUserName.value = prefs.getString("google_user_name", null)
        googleUserPhotoUrl.value = prefs.getString("google_user_photo_url", null)
    }

    fun onGoogleSignInSuccess(email: String, displayName: String?, photoUrl: String?) {
        googleUserEmail.value = email
        googleUserName.value = displayName
        googleUserPhotoUrl.value = photoUrl
        prefs.edit().apply {
            putString("google_user_email", email)
            putString("google_user_name", displayName)
            putString("google_user_photo_url", photoUrl)
        }.apply()
    }

    fun onGoogleSignOut() {
        googleUserEmail.value = null
        googleUserName.value = null
        googleUserPhotoUrl.value = null
        prefs.edit().apply {
            remove("google_user_email")
            remove("google_user_name")
            remove("google_user_photo_url")
        }.apply()
    }

    private val repositoryAllRecordsFlow = repository.allRecords

    // Settings actions
    fun setAppThemeMode(mode: Int) {
        appThemeMode.value = mode
        prefs.edit().putInt("app_theme_mode", mode).apply()
    }

    fun setWeeklyClosureDay(day: Int) {
        weeklyClosureDay.value = day
        prefs.edit().putInt("weekly_closure_day", day).apply()
        // Refresh range by re-setting current value
        referenceDateMillis.value = referenceDateMillis.value
    }

    fun setMonthlyRevenueGoal(goal: Double) {
        monthlyRevenueGoal.value = goal
        prefs.edit().putFloat("monthly_revenue_goal", goal.toFloat()).apply()
    }

    fun setDriveFolderName(name: String) {
        driveFolderName.value = name
        prefs.edit().putString("drive_folder_name", name).apply()
    }

    fun setReportFormat(format: String) {
        reportFormat.value = format
        prefs.edit().putString("report_format", format).apply()
    }

    fun setVehicleType(type: Int) {
        vehicleType.value = type
        prefs.edit().putInt("vehicle_type", type).apply()
        // If changing to electric, default consumption/price to standard electric if they are defaults
        if (type == 2 && vehicleConsumption.value == 10.0) {
            setVehicleConsumption(6.0) // 6 km/kWh is a common EV average
        } else if (type != 2 && vehicleConsumption.value == 6.0) {
            setVehicleConsumption(10.0) // Reset to standard Km/L
        }
        if (type == 2 && fuelPriceEstimate.value == 5.50) {
            setFuelPriceEstimate(0.90) // ~0.90 R$ per kWh in BR
        } else if (type != 2 && fuelPriceEstimate.value == 0.90) {
            setFuelPriceEstimate(5.50)
        }
    }

    fun setVehicleConsumption(cons: Double) {
        vehicleConsumption.value = cons
        prefs.edit().putFloat("vehicle_consumption", cons.toFloat()).apply()
    }

    fun setFuelPriceEstimate(price: Double) {
        fuelPriceEstimate.value = price
        prefs.edit().putFloat("fuel_price_estimate", price.toFloat()).apply()
    }

    fun setVehicleOwnership(ownership: Int) {
        vehicleOwnership.value = ownership
        prefs.edit().putInt("vehicle_ownership", ownership).apply()
    }

    fun setVehicleRentCost(cost: Double) {
        vehicleRentCost.value = cost
        prefs.edit().putFloat("vehicle_rent_cost", cost.toFloat()).apply()
    }

    fun setVehicleRentWeeklyCost(cost: Double) {
        vehicleRentWeeklyCost.value = cost
        prefs.edit().putFloat("vehicle_rent_weekly_cost", cost.toFloat()).apply()
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

    // Monthly absolute revenue tracker for current calendar month
    val monthlyRevenueProgress: StateFlow<Double> = repositoryAllRecordsFlow.map { records ->
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        
        records.filter { record ->
            cal.timeInMillis = record.dateMillis
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.revenue }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // UI States
    val currentPeriod = MutableStateFlow(PeriodView.DAILY)
    val referenceDateMillis = MutableStateFlow(System.currentTimeMillis())

    // All records Flow from DB
    val allRecords: StateFlow<List<DriverRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current records filtered by selected period
    // Uses combine to emit fresh data whenever currentPeriod, referenceDateMillis, weeklyClosureDay or repository data updates
    val filteredRecords: StateFlow<List<DriverRecord>> = combine(
        repositoryAllRecordsFlow,
        currentPeriod,
        referenceDateMillis,
        weeklyClosureDay
    ) { records, period, refMillis, _ ->
        val (start, end) = getPeriodRange(period, refMillis)
        records.filter { it.dateMillis in start..end }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current summary calculations for active period
    val periodSummary: StateFlow<PeriodSummary> = combine(
        filteredRecords,
        currentPeriod,
        referenceDateMillis,
        weeklyClosureDay
    ) { records, period, refMillis, _ ->
        val periodLabel = getPeriodLabel(period, refMillis)
        if (records.isEmpty()) {
            PeriodSummary(selectedPeriodType = period, periodLabel = periodLabel)
        } else {
            val totalRev = records.sumOf { it.revenue }
            val totalHrs = records.sumOf { it.hoursWorked }
            val totalKm = records.sumOf { it.kmDriven }
            val totalMeal = records.sumOf { it.expenseMeal }
            val totalRent = records.sumOf { it.expenseRent }
            val totalMisc = records.sumOf { it.expenseMisc }
            val totalFuel = records.sumOf { it.expenseFuel }
            val totalExp = totalMeal + totalRent + totalMisc + totalFuel
            val net = totalRev - totalExp

            PeriodSummary(
                selectedPeriodType = period,
                periodLabel = periodLabel,
                totalRevenue = totalRev,
                totalHours = totalHrs,
                totalKm = totalKm,
                totalMeal = totalMeal,
                totalRent = totalRent,
                totalMisc = totalMisc,
                totalFuel = totalFuel,
                totalExpenses = totalExp,
                netProfit = net,
                grossHourlyRate = if (totalHrs > 0) totalRev / totalHrs else 0.0,
                netHourlyRate = if (totalHrs > 0) net / totalHrs else 0.0,
                netMinutelyRate = if (totalHrs > 0) net / (totalHrs * 60.0) else 0.0,
                grossRevenuePerKm = if (totalKm > 0) totalRev / totalKm else 0.0,
                costPerKm = if (totalKm > 0) totalExp / totalKm else 0.0,
                profitPerKm = if (totalKm > 0) net / totalKm else 0.0
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeriodSummary(PeriodView.DAILY, "")
    )

    // For keeping track of last entered rent value to help pre-fill
    val lastRentValue: StateFlow<Double> = repositoryAllRecordsFlow
        .map { records ->
            records.firstOrNull { it.expenseRent > 0 }?.expenseRent ?: 0.0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Modify target record
    private val _editingRecord = MutableStateFlow<DriverRecord?>(null)
    val editingRecord = _editingRecord.asStateFlow()

    fun selectRecordForEdit(record: DriverRecord?) {
        _editingRecord.value = record
    }

    // Stopwatch for tracking active ride hours
    val stopwatchSeconds = StopwatchState.stopwatchSeconds
    val isStopwatchRunning = StopwatchState.isStopwatchRunning

    private fun sendServiceAction(action: String) {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.StopwatchService::class.java).apply {
            this.action = action
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startStopwatch() {
        sendServiceAction(com.example.StopwatchService.ACTION_START_WORK)
    }

    fun pauseStopwatch() {
        sendServiceAction(com.example.StopwatchService.ACTION_PAUSE_WORK)
    }

    fun resetStopwatch() {
        StopwatchState.stopwatchSeconds.value = 0L
        sendServiceAction(com.example.StopwatchService.ACTION_PAUSE_WORK)
    }

    // Secondary stopwatch for tracking lunch/break hours (tempo parado)
    val lunchStopwatchSeconds = StopwatchState.lunchStopwatchSeconds
    val isLunchStopwatchRunning = StopwatchState.isLunchStopwatchRunning

    fun startLunchStopwatch() {
        sendServiceAction(com.example.StopwatchService.ACTION_START_LUNCH)
    }

    fun pauseLunchStopwatch() {
        sendServiceAction(com.example.StopwatchService.ACTION_PAUSE_LUNCH)
    }

    fun resetLunchStopwatch() {
        StopwatchState.lunchStopwatchSeconds.value = 0L
        sendServiceAction(com.example.StopwatchService.ACTION_PAUSE_LUNCH)
    }

    // Operations
    fun saveRecord(
        id: Long = 0,
        dateMillis: Long,
        hoursWorked: Double,
        kmDriven: Double,
        revenue: Double,
        expenseMeal: Double,
        expenseRent: Double,
        expenseMisc: Double,
        expenseFuel: Double,
        description: String
    ) {
        viewModelScope.launch {
            val record = DriverRecord(
                id = id,
                dateMillis = dateMillis,
                hoursWorked = hoursWorked,
                kmDriven = kmDriven,
                revenue = revenue,
                expenseMeal = expenseMeal,
                expenseRent = expenseRent,
                expenseMisc = expenseMisc,
                expenseFuel = expenseFuel,
                description = description
            )
            if (id == 0L) {
                repository.insert(record)
            } else {
                repository.update(record)
            }
        }
    }

    fun deleteRecordById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    // Navigation and shifting periods
    fun shiftPeriod(amount: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = referenceDateMillis.value
        when (currentPeriod.value) {
            PeriodView.DAILY -> cal.add(Calendar.DAY_OF_YEAR, amount)
            PeriodView.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, amount)
            PeriodView.MONTHLY -> cal.add(Calendar.MONTH, amount)
        }
        referenceDateMillis.value = cal.timeInMillis
    }

    fun setPeriodType(type: PeriodView) {
        currentPeriod.value = type
    }

    fun setReferenceDate(millis: Long) {
        referenceDateMillis.value = millis
    }

    // Helper functions for calendar logic
    private fun getPeriodRange(period: PeriodView, refMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = refMillis
        
        return when (period) {
            PeriodView.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            PeriodView.WEEKLY -> {
                // Adjust week start to the user-selected weekly closure/starting day
                val startDay = weeklyClosureDay.value
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var diff = currentDay - startDay
                if (diff < 0) {
                    diff += 7
                }
                cal.add(Calendar.DAY_OF_YEAR, -diff)

                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            PeriodView.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
        }
    }

    private fun getPeriodLabel(period: PeriodView, refMillis: Long): String {
        val date = Date(refMillis)
        return when (period) {
            PeriodView.DAILY -> {
                val sdf = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
                sdf.format(date).replaceFirstChar { it.uppercase() }
            }
            PeriodView.WEEKLY -> {
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = refMillis
                val startDay = weeklyClosureDay.value
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var diff = currentDay - startDay
                if (diff < 0) {
                    diff += 7
                }
                cal.add(Calendar.DAY_OF_YEAR, -diff)

                val startSdf = SimpleDateFormat("d 'de' MMM", Locale("pt", "BR"))
                val startStr = startSdf.format(cal.time)

                cal.add(Calendar.DAY_OF_YEAR, 6)
                val endSdf = SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("pt", "BR"))
                val endStr = endSdf.format(cal.time)
                "Semana de $startStr a $endStr"
            }
            PeriodView.MONTHLY -> {
                val sdf = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
                sdf.format(date).replaceFirstChar { it.uppercase() }
            }
        }
    }
}
