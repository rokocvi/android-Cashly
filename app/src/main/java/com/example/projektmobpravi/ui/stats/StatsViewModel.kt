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
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadStats()
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadStats()
    }

    private fun loadStats() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.getAllTransactions(userId).collectLatest { transactions ->
                val period = _uiState.value.selectedPeriod
                val selectedCategory = _uiState.value.selectedCategory
                val startDate = getStartDate(period)
                val endDate = getEndDate(period)

                val periodFiltered = transactions.filter {
                    it.date >= startDate && it.date < endDate
                }

                val allCategories = periodFiltered.map { it.category }.distinct().sorted()

                val filtered = if (selectedCategory != null) {
                    periodFiltered.filter { it.category == selectedCategory }
                } else {
                    periodFiltered
                }

                val total = filtered.sumOf { it.amount }

                val categoryMap = filtered
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val categoryStats = categoryMap.map { (category, amount) ->
                    CategoryStat(
                        category = category,
                        amount = amount,
                        percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                    )
                }.sortedByDescending { it.amount }

                val dailyMap = filtered
                    .groupBy { transaction ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = transaction.date
                        "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}."
                    }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val dailyStats = dailyMap.map { (day, amount) ->
                    DailyStat(day = day, amount = amount)
                }.takeLast(7)

                val lastMonthStart = getLastMonthStart()
                val lastMonthEnd = getStartDate(Period.THIS_MONTH)
                val lastMonthTotal = transactions
                    .filter { it.date >= lastMonthStart && it.date < lastMonthEnd }
                    .sumOf { it.amount }

                val daysInPeriod = getDaysInPeriod(period)
                val averagePerDay = if (daysInPeriod > 0) total / daysInPeriod else 0.0
                val biggestCategory = categoryStats.firstOrNull()?.category ?: ""

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allCategories = allCategories,
                    categoryStats = categoryStats,
                    dailyStats = dailyStats,
                    totalThisMonth = total,
                    totalLastMonth = lastMonthTotal,
                    averagePerDay = averagePerDay,
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