package com.example.projektmobpravi.data.local

import android.content.Context
import com.example.projektmobpravi.domain.model.CustomCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomCategoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("custom_categories", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCategories(): List<CustomCategory> {
        val json = prefs.getString("categories", null) ?: return emptyList()
        val type = object : TypeToken<List<CustomCategory>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveCategory(category: CustomCategory) {
        val current = getCategories().toMutableList()
        if (current.none { it.name.equals(category.name, ignoreCase = true) }) {
            current.add(category)
            prefs.edit().putString("categories", gson.toJson(current)).apply()
        }
    }

    fun deleteCategory(name: String) {
        val updated = getCategories().filter { !it.name.equals(name, ignoreCase = true) }
        prefs.edit().putString("categories", gson.toJson(updated)).apply()
    }
}
