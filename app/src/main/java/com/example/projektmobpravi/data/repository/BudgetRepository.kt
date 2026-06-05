package com.example.projektmobpravi.data.repository

import com.example.projektmobpravi.data.local.dao.BudgetDao
import com.example.projektmobpravi.data.local.entity.BudgetEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val firestore: FirebaseFirestore
) {

    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>> =
        budgetDao.getAllByUser(userId)

    suspend fun getBudgetForCategory(userId: String, category: String): BudgetEntity? =
        budgetDao.getBudget(userId, category)

    suspend fun setBudget(userId: String, category: String, limit: Double) {
        val entity = BudgetEntity(userId = userId, category = category, limitAmount = limit)
        budgetDao.upsert(entity)
        firestore.collection("budgets")
            .document("${userId}_${category}")
            .set(entity)
            .await()
    }

    suspend fun removeBudget(userId: String, category: String) {
        budgetDao.delete(userId, category)
        firestore.collection("budgets")
            .document("${userId}_${category}")
            .delete()
            .await()
    }

    suspend fun syncFromFirestore(userId: String) {
        val snapshot = firestore.collection("budgets")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Firebase je source of truth — obriši lokalne i re-insertiraj
        budgetDao.deleteAllByUser(userId)

        snapshot.documents.forEach { doc ->
            val data = doc.data ?: return@forEach
            val budget = BudgetEntity(
                userId = data["userId"] as? String ?: return@forEach,
                category = data["category"] as? String ?: return@forEach,
                limitAmount = (data["limitAmount"] as? Double)
                    ?: (data["limitAmount"] as? Long)?.toDouble()
                    ?: return@forEach
            )
            budgetDao.upsert(budget)
        }
    }
}
