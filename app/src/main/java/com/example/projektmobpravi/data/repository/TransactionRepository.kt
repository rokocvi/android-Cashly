package com.example.projektmobpravi.data.repository

import com.example.projektmobpravi.BuildConfig
import com.example.projektmobpravi.data.local.dao.TransactionDao
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.data.remote.api.ExchangeRatesApi
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val exchangeRatesApi: ExchangeRatesApi,
    private val firestore: FirebaseFirestore
) {

    // ───── Room (lokalni cache) ─────

    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getAllByUser(userId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        // Generiraj doc ID unaprijed da ga možemo odmah spremiti i lokalno i na Firebase
        val docRef = firestore.collection("transactions").document()
        val withFirebaseId = transaction.copy(firebaseId = docRef.id)
        docRef.set(withFirebaseId).await()
        transactionDao.insert(withFirebaseId)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        if (transaction.firebaseId.isNotEmpty()) {
            firestore.collection("transactions")
                .document(transaction.firebaseId)
                .delete()
                .await()
        }
        transactionDao.delete(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        if (transaction.firebaseId.isNotEmpty()) {
            firestore.collection("transactions")
                .document(transaction.firebaseId)
                .set(transaction)
                .await()
        }
        transactionDao.update(transaction)
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return transactionDao.getById(id)
    }

    fun getByCategory(userId: String, category: String): Flow<List<TransactionEntity>> {
        return transactionDao.getByCategory(userId, category)
    }

    fun getTotalSpentSince(userId: String, startDate: Long): Flow<Double?> {
        return transactionDao.getTotalSpentSince(userId, startDate)
    }

    suspend fun getTotalByCategoryInMonth(userId: String, category: String, startDate: Long): Double? {
        return transactionDao.getTotalByCategoryInMonth(userId, category, startDate)
    }

    fun getTotalByCattreegory(userId: String, category: String): Flow<Double?> {
        return transactionDao.getTotalByCategory(userId, category)
    }

    // ───── Retrofit (Exchange Rates) ─────

    suspend fun getExchangeRates(): Map<String, Double> {
        val response = exchangeRatesApi.getLatestRates(
            apiKey = BuildConfig.EXCHANGE_RATE_API_KEY
        )
        return response.rates
    }

    // ───── Firebase (sync) ─────

    suspend fun syncTransactionsFromFirebase(userId: String) {
        val snapshot = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Firebase je source of truth — obriši lokalne i re-insertiraj
        transactionDao.deleteAllByUser(userId)

        snapshot.documents.forEach { doc ->
            val data = doc.data ?: return@forEach
            val transaction = TransactionEntity(
                firebaseId = doc.id,
                userId = data["userId"] as? String ?: return@forEach,
                amount = (data["amount"] as? Double)
                    ?: (data["amount"] as? Long)?.toDouble()
                    ?: return@forEach,
                category = data["category"] as? String ?: return@forEach,
                note = data["note"] as? String ?: "",
                date = data["date"] as? Long ?: return@forEach,
                currency = data["currency"] as? String ?: "EUR"
            )
            transactionDao.insert(transaction)
        }
    }
}