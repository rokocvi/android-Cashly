package com.example.projektmobpravi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList(),
    val totalThisMonth: Double = 0.0,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val username: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid ?: return
        val username = auth.currentUser?.displayName
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Korisnik"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, username = username)

            // Sync s Firebaseom pri pokretanju
            try {
                repository.syncTransactionsFromFirebase(userId)
            } catch (e: Exception) {
                // Nastavi s lokalnim podacima ako sync ne uspije
            }

            // Promatraj lokalne transakcije
            repository.getAllTransactions(userId).collectLatest { transactions ->
                val startOfMonth = getStartOfMonth()
                val thisMonthTransactions = transactions.filter { it.date >= startOfMonth }
                val totalThisMonth = thisMonthTransactions.sumOf { it.amount }

                // Suma po kategorijama za ovaj mjesec
                val categoryTotals = thisMonthTransactions
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    transactions = transactions.take(10), // zadnjih 10
                    totalThisMonth = totalThisMonth,
                    categoryTotals = categoryTotals
                )
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun logout() {
        auth.signOut()
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