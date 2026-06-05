package com.example.projektmobpravi.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmobpravi.data.repository.BudgetRepository
import com.example.projektmobpravi.data.repository.TransactionRepository
import com.example.projektmobpravi.domain.model.Category
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetItem(
    val category: Category,
    val limitAmount: Double?,
    val spentThisMonth: Double
) {
    val progress: Float
        get() = if (limitAmount != null && limitAmount > 0)
            (spentThisMonth / limitAmount).toFloat().coerceAtMost(1f)
        else 0f

    val isOverBudget: Boolean
        get() = limitAmount != null && spentThisMonth > limitAmount
}

data class BudgetUiState(
    val isLoading: Boolean = true,
    val items: List<BudgetItem> = emptyList()
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                budgetRepository.syncFromFirestore(userId)
            } catch (e: Exception) {
                // Nastavi s lokalnim podacima ako sync ne uspije
            }

            combine(
                budgetRepository.getAllBudgets(userId),
                transactionRepository.getAllTransactions(userId)
            ) { budgets, transactions ->
                val startOfMonth = getStartOfMonth()
                val spentByCategory = transactions
                    .filter { it.date >= startOfMonth }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val budgetMap = budgets.associate { it.category to it.limitAmount }

                Category.entries.map { cat ->
                    BudgetItem(
                        category = cat,
                        limitAmount = budgetMap[cat.displayName],
                        spentThisMonth = spentByCategory[cat.displayName] ?: 0.0
                    )
                }
            }.collect { items ->
                _uiState.value = BudgetUiState(isLoading = false, items = items)
            }
        }
    }

    fun setBudget(category: Category, limit: Double) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            budgetRepository.setBudget(userId, category.displayName, limit)
        }
    }

    fun removeBudget(category: Category) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            budgetRepository.removeBudget(userId, category.displayName)
        }
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
