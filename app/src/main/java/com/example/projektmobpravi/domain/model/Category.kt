package com.example.projektmobpravi.domain.model

enum class Category(val displayName: String, val emoji: String) {
    HRANA("Hrana", "🍔"),
    TRGOVINA("Trgovina", "🛒"),
    PRIJEVOZ("Prijevoz", "🚗"),
    ZABAVA("Zabava", "🎬"),
    KUCA("Kuća", "🏠"),
    ZDRAVLJE("Zdravlje", "💊"),
    ODJEVANJE("Odijevanje", "👕"),
    LJEPOTA("Ljepota", "💇"),
    OSTALO("Ostalo", "📦")
}