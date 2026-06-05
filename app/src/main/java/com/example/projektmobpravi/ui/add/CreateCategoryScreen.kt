package com.example.projektmobpravi.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📦") }

    Scaffold(containerColor = SurfaceLight) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(colors = listOf(DeepGreen, MintGreen))
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Nazad",
                            tint = TextOnDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nova kategorija",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextOnDark
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = selectedEmoji, fontSize = 44.sp)
                        Column {
                            Text(
                                text = if (name.isEmpty()) "Naziv kategorije" else name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (name.isEmpty()) TextMuted else TextDark
                            )
                            Text(
                                text = "Pregled",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Naziv
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Naziv",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 20) name = it },
                            label = { Text("npr. Teretana, Kućni ljubimci...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintGreen,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedLabelColor = MintGreen
                            ),
                            singleLine = true,
                            supportingText = { Text("${name.length}/20", color = TextMuted) }
                        )
                    }
                }

                // Emoji picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Odaberi emoji",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        EMOJI_OPTIONS.chunked(6).forEach { rowEmojis ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEmojis.forEach { emoji ->
                                    val isSelected = selectedEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) DeepGreen.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) DeepGreen
                                                else TextMuted.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedEmoji = emoji }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "newCategoryName", name.trim()
                        )
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "newCategoryEmoji", selectedEmoji
                        )
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = "Spremi kategoriju",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
