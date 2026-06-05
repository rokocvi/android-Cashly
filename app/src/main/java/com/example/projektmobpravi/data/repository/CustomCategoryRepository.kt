package com.example.projektmobpravi.data.repository

import com.example.projektmobpravi.data.local.CustomCategoryStore
import com.example.projektmobpravi.domain.model.CustomCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomCategoryRepository @Inject constructor(
    private val store: CustomCategoryStore,
    private val firestore: FirebaseFirestore
) {
    private fun ref(userId: String) =
        firestore.collection("customCategories").document(userId).collection("items")

    fun getLocalCategories(): List<CustomCategory> = store.getCategories()

    suspend fun loadFromFirestore(userId: String): List<CustomCategory> {
        return try {
            val snapshot = ref(userId).get().await()
            val remote = snapshot.toObjects(CustomCategory::class.java)
            remote.forEach { store.saveCategory(it) }
            store.getCategories()
        } catch (e: Exception) {
            store.getCategories()
        }
    }

    suspend fun saveCategory(userId: String, category: CustomCategory) {
        store.saveCategory(category)
        try {
            ref(userId).document(category.name).set(category).await()
        } catch (e: Exception) {
            // lokalno već spremljeno, Firestore će syncati kad bude online
        }
    }

    suspend fun deleteCategory(userId: String, name: String) {
        store.deleteCategory(name)
        try {
            ref(userId).document(name).delete().await()
        } catch (e: Exception) {
            // lokalno već obrisano
        }
    }
}
