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
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val netHourlyRate: Double = 0.0,
    val netMinutelyRate: Double = 0.0,
    val costPerKm: Double = 0.0,
    val profitPerKm: Double = 0.0
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DriverRepository

    init {
        val database = DriverDatabase.getDatabase(application)
        repository = DriverRepository(database.dao)
    }

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

    private val repositoryAllRecordsFlow = repository.allRecords

    // Current records filtered by selected period
    // Uses combine to emit fresh data whenever currentPeriod, referenceDateMillis or repository data updates
    val filteredRecords: StateFlow<List<DriverRecord>> = combine(
        repositoryAllRecordsFlow,
        currentPeriod,
        referenceDateMillis
    ) { records, period, refMillis ->
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
        referenceDateMillis
    ) { records, period, refMillis ->
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
            val totalExp = totalMeal + totalRent + totalMisc
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
                totalExpenses = totalExp,
                netProfit = net,
                netHourlyRate = if (totalHrs > 0) net / totalHrs else 0.0,
                netMinutelyRate = if (totalHrs > 0) net / (totalHrs * 60.0) else 0.0,
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
    private var stopwatchJob: Job? = null
    val stopwatchSeconds = MutableStateFlow(0L)
    val isStopwatchRunning = MutableStateFlow(false)

    fun startStopwatch() {
        if (isStopwatchRunning.value) return
        // Auto pause lunch if it is running
        if (isLunchStopwatchRunning.value) {
            pauseLunchStopwatch()
        }
        isStopwatchRunning.value = true
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (isStopwatchRunning.value) {
                delay(1000)
                stopwatchSeconds.value += 1
            }
        }
    }

    fun pauseStopwatch() {
        isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        stopwatchJob = null
    }

    fun resetStopwatch() {
        pauseStopwatch()
        stopwatchSeconds.value = 0L
    }

    // Secondary stopwatch for tracking lunch/break hours (tempo parado)
    private var lunchStopwatchJob: Job? = null
    val lunchStopwatchSeconds = MutableStateFlow(0L)
    val isLunchStopwatchRunning = MutableStateFlow(false)

    fun startLunchStopwatch() {
        if (isLunchStopwatchRunning.value) return
        // Auto pause work stopwatch if it is running
        if (isStopwatchRunning.value) {
            pauseStopwatch()
        }
        isLunchStopwatchRunning.value = true
        lunchStopwatchJob?.cancel()
        lunchStopwatchJob = viewModelScope.launch {
            while (isLunchStopwatchRunning.value) {
                delay(1000)
                lunchStopwatchSeconds.value += 1
            }
        }
    }

    fun pauseLunchStopwatch() {
        isLunchStopwatchRunning.value = false
        lunchStopwatchJob?.cancel()
        lunchStopwatchJob = null
    }

    fun resetLunchStopwatch() {
        pauseLunchStopwatch()
        lunchStopwatchSeconds.value = 0L
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
                // Set first day of week. In Brazil, standard calendar has Sunday or Monday.
                // Let's compute based on firstDayOfWeek so it is localized, or explicitly Monday.
                // Let's align to Monday as start of week.
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
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
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
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
