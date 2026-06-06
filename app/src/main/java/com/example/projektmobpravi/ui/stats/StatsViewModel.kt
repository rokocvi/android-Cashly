package com.example.projektmobpravi.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmobpravi.data.repository.TransactionRepository
import com.example.projektmobpravi.util.CsvExporter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class DailyStat(
    val day: String,
    val amount: Double
)

data class StatsUiState(
    val isLoading: Boolean = false,
    val allCategories: List<String> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val dailyStats: List<DailyStat> = emptyList(),
    val totalThisMonth: Double = 0.0,
    val totalLastMonth: Double = 0.0,
    val averagePerDay: Double = 0.0,
    val biggestCategory: String = "",
    val selectedPeriod: Period = Period.THIS_MONTH,
    val selectedCategory: String? = null,
    val selectedDate: Long? = null,
    val exportSuccess: Boolean = false,
    val exportError: Boolean = false
)

enum class Period(val displayName: String) {
    THIS_WEEK("Ovaj tjedan"),
    THIS_MONTH("Ovaj mjesec"),
    LAST_MONTH("Prošli mjesec")
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadStats()
    }

    fun selectPeriod(period: Period) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period, selectedDate = null)
        loadStats()
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadStats()
    }

    fun selectDate(date: Long?) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadStats()
    }

    private fun loadStats() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllTransactions(userId).collectLatest { transactions ->
                val period          = _uiState.value.selectedPeriod
                val selectedCategory = _uiState.value.selectedCategory
                val selectedDate    = _uiState.value.selectedDate

                // Base filter: single day OR period
                val baseFiltered = if (selectedDate != null) {
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utcCal.timeInMillis = selectedDate
                    val localCal = Calendar.getInstance()
                    localCal.set(
                        utcCal.get(Calendar.YEAR),
                        utcCal.get(Calendar.MONTH),
                        utcCal.get(Calendar.DAY_OF_MONTH),
                        0, 0, 0
                    )
                    localCal.set(Calendar.MILLISECOND, 0)
                    val dayStart = localCal.timeInMillis
                    val dayEnd   = dayStart + 24 * 60 * 60 * 1000L
                    transactions.filter { it.date >= dayStart && it.date < dayEnd }
                } else {
                    val startDate = getStartDate(period)
                    val endDate   = getEndDate(period)
                    transactions.filter { it.date >= startDate && it.date < endDate }
                }

                val allCategories = baseFiltered.map { it.category }.distinct().sorted()

                val filtered = if (selectedCategory != null) {
                    baseFiltered.filter { it.category == selectedCategory }
                } else {
                    baseFiltered
                }

                val total = filtered.sumOf { it.amount }

                val categoryMap = filtered
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val categoryStats = categoryMap.map { (category, amount) ->
                    CategoryStat(
                        category   = category,
                        amount     = amount,
                        percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                    )
                }.sortedByDescending { it.amount }

                val dailyStats = if (selectedDate != null) {
                    emptyList()
                } else {
                    val dailyMap = filtered
                        .groupBy { transaction ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = transaction.date
                            "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}."
                        }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                    dailyMap.map { (day, amount) -> DailyStat(day = day, amount = amount) }.takeLast(7)
                }

                val lastMonthStart = getLastMonthStart()
                val lastMonthEnd   = getStartDate(Period.THIS_MONTH)
                val lastMonthTotal = transactions
                    .filter { it.date >= lastMonthStart && it.date < lastMonthEnd }
                    .sumOf { it.amount }

                val daysInPeriod   = getDaysInPeriod(period)
                val averagePerDay  = if (daysInPeriod > 0) total / daysInPeriod else 0.0
                val biggestCategory = categoryStats.firstOrNull()?.category ?: ""

                _uiState.value = _uiState.value.copy(
                    isLoading       = false,
                    allCategories   = allCategories,
                    categoryStats   = categoryStats,
                    dailyStats      = dailyStats,
                    totalThisMonth  = total,
                    totalLastMonth  = lastMonthTotal,
                    averagePerDay   = averagePerDay,
                    biggestCategory = biggestCategory
                )
            }
        }
    }

    fun exportTransactions(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val transactions = repository.getAllTransactions(userId).first()
            val success = CsvExporter.export(context, transactions)
            _uiState.update { it.copy(exportSuccess = success, exportError = !success) }
        }
    }

    fun clearExportState() {
        _uiState.update { it.copy(exportSuccess = false, exportError = false) }
    }

    private fun getEndDate(period: Period): Long {
        val calendar = Calendar.getInstance()
        return when (period) {
            Period.THIS_WEEK, Period.THIS_MONTH -> {
                // Do danas
                System.currentTimeMillis()
            }
            Period.LAST_MONTH -> {
                // Do početka ovog mjeseca
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
        }
    }

    private fun getStartDate(period: Period): Long {
        val calendar = Calendar.getInstance()
        return when (period) {
            Period.THIS_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            Period.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            Period.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
        }
    }

    private fun getLastMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDaysInPeriod(period: Period): Int {
        return when (period) {
            Period.THIS_WEEK -> 7
            Period.THIS_MONTH -> Calendar.getInstance()
                .getActualMaximum(Calendar.DAY_OF_MONTH)
            Period.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
    }
}