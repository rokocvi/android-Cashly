package com.example.projektmobpravi.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.domain.model.Category
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.home.getCategoryColor
import com.example.projektmobpravi.ui.home.getCategoryEmoji
import com.example.projektmobpravi.ui.theme.*

@Composable
fun BudgetScreen(navController: NavHostController) {
    val viewModel: BudgetViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingCategory by remember { mutableStateOf<Category?>(null) }

    if (editingCategory != null) {
        BudgetEditDialog(
            category = editingCategory!!,
            currentLimit = uiState.items.find { it.category == editingCategory }?.limitAmount,
            onConfirm = { limit ->
                if (limit != null) viewModel.setBudget(editingCategory!!, limit)
                else viewModel.removeBudget(editingCategory!!)
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    BudgetHeader()
                }

                item {
                    BudgetSummaryCard(items = uiState.items)
                }

                item {
                    Text(
                        text = "Budgeti po kategoriji",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                items(uiState.items) { item ->
                    BudgetCategoryCard(
                        item = item,
                        onClick = { editingCategory = item.category }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepGreen, MintGreen)
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "Budgeti",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
            )
            Text(
                text = "Postavi limite po kategoriji",
                fontSize = 13.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(items: List<BudgetItem>) {
    val budgetedItems = items.filter { it.limitAmount != null }
    val totalBudget = budgetedItems.sumOf { it.limitAmount ?: 0.0 }
    val totalSpent = budgetedItems.sumOf { it.spentThisMonth }
    val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceAtMost(1f) else 0f
    val overBudgetCount = budgetedItems.count { it.isOverBudget }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Ukupni budget ovaj mjesec",
                fontSize = 14.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "€%.2f".format(totalSpent),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnDark
                )
                Text(
                    text = " / €%.2f".format(totalBudget),
                    fontSize = 16.sp,
                    color = TextOnDark.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (overallProgress >= 1f) ErrorRed else AccentGold,
                trackColor = TextOnDark.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (overBudgetCount > 0) {
                Text(
                    text = "⚠️ $overBudgetCount ${if (overBudgetCount == 1) "kategorija" else "kategorije"} prešla limit",
                    fontSize = 12.sp,
                    color = Color(0xFFFFD700)
                )
            } else if (budgetedItems.isEmpty()) {
                Text(
                    text = "💡 Dodiri kategoriju da postaviš limit",
                    fontSize = 12.sp,
                    color = AccentGold
                )
            } else {
                Text(
                    text = "✅ Unutar budgeta",
                    fontSize = 12.sp,
                    color = Color(0xFF90EE90)
                )
            }
        }
    }
}

@Composable
fun BudgetCategoryCard(item: BudgetItem, onClick: () -> Unit) {
    val categoryColor = getCategoryColor(item.category.displayName)
    val progressColor = when {
        item.isOverBudget -> ErrorRed
        item.progress >= 0.7f -> AccentGold
        else -> SuccessGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(item.category.displayName),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    if (item.limitAmount != null) {
                        Text(
                            text = "€%.2f / €%.2f".format(item.spentThisMonth, item.limitAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (item.isOverBudget) ErrorRed else TextMuted
                        )
                    } else {
                        Text(
                            text = "€%.2f  —  nema limita".format(item.spentThisMonth),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                if (item.limitAmount != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = categoryColor.copy(alpha = 0.15f)
                    )
                    if (item.isOverBudget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prekoračen za €%.2f".format(item.spentThisMonth - item.limitAmount),
                            fontSize = 11.sp,
                            color = ErrorRed
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dodiri za postavljanje limita",
                        fontSize = 11.sp,
                        color = TextMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetEditDialog(
    category: Category,
    currentLimit: Double?,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf(currentLimit?.let { "%.2f".format(it) } ?: "") }
    val isValid = inputText.toDoubleOrNull()?.let { it > 0 } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${getCategoryEmoji(category.displayName)} ${category.displayName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Postavi mjesečni limit za ovu kategoriju:",
                    fontSize = 14.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Limit (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        focusedLabelColor = DeepGreen
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(inputText.toDoubleOrNull()) },
                enabled = isValid
            ) {
                Text("Spremi", color = if (isValid) DeepGreen else TextMuted)
            }
        },
        dismissButton = {
            Row {
                if (currentLimit != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text("Ukloni limit", color = ErrorRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Odustani")
                }
            }
        }
    )
}
