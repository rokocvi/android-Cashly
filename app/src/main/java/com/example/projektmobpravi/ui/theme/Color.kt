package com.example.projektmobpravi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── Primarne boje — konstantne (ne mijenjaju se s temom) ───────────────────
val DeepGreen  = Color(0xFF1A1040)   // Deep indigo — headeri, primary gumbi
val MintGreen  = Color(0xFF7C3AED)   // Vibrant violet — akcenti, forme, ikone
val BrandEnd   = Color(0xFF4527A0)   // Kraj gradijenta u headerima
val AccentGold = Color(0xFFF79009)   // Jantarni akcent
val TextOnDark = Color(0xFFFFFFFF)   // Bijela na tamnim površinama

// ── Površine — mijenjaju se s temom ────────────────────────────────────────
val SurfaceLight: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.surfaceLight

val SurfaceCard: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.surfaceCard

// ── Tekst — mijenja se s temom ─────────────────────────────────────────────
val TextDark: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.textDark

val TextMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.textMuted

// ── Status ─────────────────────────────────────────────
val ErrorRed     = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF12B76A)

// ── Container boje ─────────────────────────────────────
val PrimaryContainer = Color(0xFFEDE9FE)  // Lagani lavender — container za indikator

// ── Kategorije ─────────────────────────────────────────
val CategoryFood          = Color(0xFFEF4444)  // Crvena
val CategoryTransport     = Color(0xFF3B82F6)  // Plava
val CategoryEntertainment = Color(0xFF8B5CF6)  // Ljubičasta
val CategoryHealth        = Color(0xFF10B981)  // Smaragdna
val CategoryHousing       = Color(0xFFF97316)  // Narančasta
val CategoryClothing      = Color(0xFFEC4899)  // Roza
val CategoryOther         = Color(0xFF6B7280)  // Siva
