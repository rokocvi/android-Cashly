package com.example.projektmobpravi.ui.add

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projektmobpravi.ui.theme.*

private val EMOJI_OPTIONS = listOf(
    "🍕", "🍔", "🍜", "☕", "🍺", "🥗",
    "🛒", "🛍️", "💄", "👗", "👟", "⌚",
    "📚", "🎮", "🎵", "🎬", "🎭", "🎨",
    "✈️", "🏖️", "⛺", "🏋️", "🧘", "🐕",
    "💊", "🏥", "⛽", "🚌", "🔧", "💡",
    "📱", "💻", "🎓", "🎁", "🌱", "🏠"
)

@Composable
fun CreateCategoryScreen(navController: NavHostController) {
    var name          by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📦") }

    Scaffold(containerColor = SurfaceLight) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(DeepGreen, Color(0xFF025C46))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.05f),
                        radius = 130.dp.toPx(),
                        center = Offset(size.width * 0.88f, -35.dp.toPx())
                    )
                }
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint               = TextOnDark,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text  = "Nova kategorija",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextOnDark
                    )
                }
            }

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Preview ──────────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DeepGreen.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = selectedEmoji, style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text(
                                text  = if (name.isEmpty()) "Naziv kategorije" else name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (name.isEmpty()) TextMuted else TextDark
                            )
                            Text(
                                text  = "Pregled",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // ── Naziv ─────────────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text  = "Naziv",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        OutlinedTextField(
                            value         = name,
                            onValueChange = { if (it.length <= 20) name = it },
                            label         = { Text("npr. Teretana, Kućni ljubimci...") },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MintGreen,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedLabelColor    = MintGreen
                            ),
                            singleLine     = true,
                            supportingText = {
                                Text(
                                    text  = "${name.length}/20",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        )
                    }
                }

                // ── Emoji picker ──────────────────────────────────────
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text  = "Odaberi emoji",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        EMOJI_OPTIONS.chunked(6).forEach { rowEmojis ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEmojis.forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) PrimaryContainer
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) DeepGreen
                                                else TextMuted.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text      = emoji,
                                            style     = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick  = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "newCategoryName", name.trim()
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "newCategoryEmoji", selectedEmoji
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                    enabled  = name.isNotBlank()
                ) {
                    Text(
                        text  = "Spremi kategoriju",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextOnDark
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
