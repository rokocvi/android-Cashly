package com.example.projektmobpravi.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.data.repository.BudgetRepository
import com.example.projektmobpravi.data.repository.CustomCategoryRepository
import com.example.projektmobpravi.data.repository.TransactionRepository
import com.example.projektmobpravi.domain.model.Category
import com.example.projektmobpravi.domain.model.CustomCategory
import com.example.projektmobpravi.util.BudgetNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val selectedCategoryName: String = Category.HRANA.displayName,
    val selectedCategoryEmoji: String = Category.HRANA.emoji,
    val customCategories: List<CustomCategory> = emptyList(),
    val exchangeRates: Map<String, Double> = emptyMap(),
    val selectedCurrency: String = "EUR",
    val isEditMode: Boolean = false,
    val initialAmount: String = "",
    val initialNote: String = "",
    val editingTransactionId: Int = -1,
    val editingFirebaseId: String = "",
    val editingUserId: String = "",
    val editingDate: Long = 0L
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val auth: FirebaseAuth,
    private val customCategoryRepository: CustomCategoryRepository,
    private val notificationHelper: BudgetNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState

    init {
        loadExchangeRates()
        _uiState.value = _uiState.value.copy(
            customCategories = customCategoryRepository.getLocalCategories()
        )
        syncCustomCategories()
    }

    private fun syncCustomCategories() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val synced = customCategoryRepository.loadFromFirestore(userId)
            _uiState.value = _uiState.value.copy(customCategories = synced)
        }
    }

    private fun loadExchangeRates() {
        viewModelScope.launch {
            try {
                val rates = repository.getExchangeRates()
                _uiState.value = _uiState.value.copy(exchangeRates = rates)
            } catch (e: Exception) {
                // Nastavi bez konverzije ako API ne radi
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryName = category.displayName,
            selectedCategoryEmoji = category.emoji
        )
    }

    fun deleteCustomCategory(name: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            customCategoryRepository.deleteCategory(userId, name)
            val remaining = customCategoryRepository.getLocalCategories()
            val stillSelected = _uiState.value.selectedCategoryName == name
            _uiState.value = _uiState.value.copy(
                customCategories = remaining,
                selectedCategoryName = if (stillSelected) Category.HRANA.displayName else _uiState.value.selectedCategoryName,
                selectedCategoryEmoji = if (stillSelected) Category.HRANA.emoji else _uiState.value.selectedCategoryEmoji
            )
        }
    }

    fun selectCustomCategory(name: String, emoji: String) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryName = name,
            selectedCategoryEmoji = emoji
        )
    }

    fun addCustomCategory(name: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        val category = CustomCategory(name, emoji)
        viewModelScope.launch {
            customCategoryRepository.saveCategory(userId, category)
            _uiState.value = _uiState.value.copy(
                customCategories = customCategoryRepository.getLocalCategories(),
                selectedCategoryName = name,
                selectedCategoryEmoji = emoji
            )
        }
    }

    fun selectCurrency(currency: String) {
        _uiState.value = _uiState.value.copy(selectedCurrency = currency)
    }

    fun loadTransactionForEdit(transactionId: Int) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(transactionId) ?: return@launch
            val categoryEmoji = Category.values()
                .firstOrNull { it.displayName == transaction.category }?.emoji
                ?: _uiState.value.customCategories
                    .firstOrNull { it.name == transaction.category }?.emoji
                ?: "📦"
            _uiState.value = _uiState.value.copy(
                isEditMode = true,
                initialAmount = "%.2f".format(transaction.amount),
                initialNote = transaction.note,
                selectedCategoryName = transaction.category,
                selectedCategoryEmoji = categoryEmoji,
                selectedCurrency = transaction.currency,
                editingTransactionId = transaction.id,
                editingFirebaseId = transaction.firebaseId,
                editingUserId = transaction.userId,
                editingDate = transaction.date
            )
        }
    }

    fun addTransaction(amountString: String, note: String) {
        val amount = amountString.replace(",", ".").toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unesite ispravan iznos")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Korisnik nije prijavljen")
            return
        }

        val amountInEur = convertToEur(amount, _uiState.value.selectedCurrency)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val category = _uiState.value.selectedCategoryName
                val startOfMonth = getStartOfMonth()

                val spentBefore = repository.getTotalByCategoryInMonth(userId, category, startOfMonth) ?: 0.0
                val budget = budgetRepository.getBudgetForCategory(userId, category)

                val transaction = TransactionEntity(
                    userId = userId,
                    amount = amountInEur,
                    category = category,
                    note = note,
                    date = System.currentTimeMillis(),
                    currency = "EUR"
                )
                repository.insertTransaction(transaction)

                if (budget != null) {
                    checkBudgetThreshold(category, spentBefore, spentBefore + amountInEur, budget.limitAmount)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška pri spremanju transakcije"
                )
            }
        }
    }

    fun updateTransaction(amountString: String, note: String) {
        val amount = amountString.replace(",", ".").toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unesite ispravan iznos")
            return
        }

        val state = _uiState.value
        val amountInEur = convertToEur(amount, state.selectedCurrency)

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            try {
                val updated = TransactionEntity(
                    id = state.editingTransactionId,
                    firebaseId = state.editingFirebaseId,
                    userId = state.editingUserId,
                    amount = amountInEur,
                    category = state.selectedCategoryName,
                    note = note,
                    date = state.editingDate,
                    currency = "EUR"
                )
                repository.updateTransaction(updated)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška pri ažuriranju transakcije"
                )
            }
        }
    }

    private fun convertToEur(amount: Double, currency: String): Double {
        if (currency == "EUR") return amount
        val rate = _uiState.value.exchangeRates[currency] ?: return amount
        return amount / rate
    }

    private fun checkBudgetThreshold(
        category: String,
        spentBefore: Double,
        spentAfter: Double,
        limit: Double
    ) {
        if (limit <= 0) return
        val progressBefore = spentBefore / limit
        val progressAfter = spentAfter / limit

        when {
            progressBefore < 1.0 && progressAfter >= 1.0 ->
                notificationHelper.notifyOverLimit(category, spentAfter, limit)
            progressBefore < 0.8 && progressAfter >= 0.8 ->
                notificationHelper.notifyNearLimit(category, (progressAfter * 100).toInt(), limit)
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
