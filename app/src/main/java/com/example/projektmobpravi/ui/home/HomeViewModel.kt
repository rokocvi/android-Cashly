package com.example.projektmobpravi.ui.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val isOffline: Boolean = false,
    val transactions: List<TransactionEntity> = emptyList(),
    val totalCount: Int = 0,
    val displayCount: Int = HomeViewModel.PAGE_SIZE,
    val totalThisMonth: Double = 0.0,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val username: String = "",
    val searchQuery: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private var allTransactions: List<TransactionEntity> = emptyList()

    companion object { const val PAGE_SIZE = 10 }

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        loadData()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                _uiState.value = _uiState.value.copy(isLoggedOut = true)
            }
        }
        auth.addAuthStateListener(listener)
        authStateListener = listener
    }

    override fun onCleared() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        super.onCleared()
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid ?: return
        val authDisplayName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        val emailFallback = auth.currentUser?.email?.substringBefore("@") ?: "Korisnik"

        viewModelScope.launch {
            // Odmah postavi ono što imamo lokalno
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                username  = authDisplayName ?: emailFallback
            )

            // Ako displayName nije u Firebase Auth, dohvati iz Firestorea
            if (authDisplayName == null) {
                try {
                    val doc = firestore.collection("users").document(userId).get().await()
                    val firestoreName = doc.getString("username")?.takeIf { it.isNotBlank() }
                    if (firestoreName != null) {
                        _uiState.value = _uiState.value.copy(username = firestoreName)
                    }
                } catch (_: Exception) { }
            }

            // Provjeri konekciju i sinkroniziraj
            val online = isOnline()
            _uiState.value = _uiState.value.copy(isOffline = !online)
            if (online) {
                try {
                    repository.syncTransactionsFromFirebase(userId)
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(isOffline = true)
                }
            }

            // Promatraj lokalne transakcije
            repository.getAllTransactions(userId).collectLatest { transactions ->
                val startOfMonth = getStartOfMonth()
                val thisMonthTransactions = transactions.filter { it.date >= startOfMonth }
                val totalThisMonth = thisMonthTransactions.sumOf { it.amount }

                val categoryTotals = thisMonthTransactions
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                allTransactions = transactions
                _uiState.value = _uiState.value.copy(
                    isLoading      = false,
                    totalCount     = transactions.size,
                    transactions   = applyFilter(transactions, _uiState.value.searchQuery, _uiState.value.displayCount),
                    totalThisMonth = totalThisMonth,
                    categoryTotals = categoryTotals
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery  = query,
            transactions = applyFilter(allTransactions, query, _uiState.value.displayCount)
        )
    }

    fun loadMore() {
        val newCount = _uiState.value.displayCount + PAGE_SIZE
        _uiState.value = _uiState.value.copy(
            displayCount = newCount,
            transactions = applyFilter(allTransactions, _uiState.value.searchQuery, newCount)
        )
    }

    private fun applyFilter(transactions: List<TransactionEntity>, query: String, displayCount: Int): List<TransactionEntity> {
        val trimmed = query.trim().lowercase()
        return if (trimmed.isEmpty()) {
            transactions.take(displayCount)
        } else {
            transactions.filter { t ->
                t.note.lowercase().contains(trimmed) ||
                t.category.lowercase().contains(trimmed) ||
                (categoryEnglish[t.category]?.contains(trimmed) == true)
            }
        }
    }

    private val categoryEnglish = mapOf(
        "Hrana"      to "food",
        "Trgovina"   to "shopping",
        "Prijevoz"   to "transport",
        "Zabava"     to "entertainment",
        "Kuća"       to "housing",
        "Zdravlje"   to "health",
        "Odijevanje" to "clothing",
        "Ljepota"    to "beauty"
    )

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun logout() {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("remember_me", false).apply()
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