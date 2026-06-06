package com.example.projektmobpravi.ui.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
            category     = editingCategory!!,
            currentLimit = uiState.items.find { it.category == editingCategory }?.limitAmount,
            onConfirm    = { limit ->
                if (limit != null) viewModel.setBudget(editingCategory!!, limit)
                else viewModel.removeBudget(editingCategory!!)
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }

    Scaffold(
        bottomBar      = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepGreen, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { BudgetHeader() }
                item { BudgetSummaryCard(items = uiState.items) }
                item {
                    Text(
                        text     = "Budgeti po kategoriji",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                items(uiState.items) { item ->
                    BudgetCategoryCard(
                        item    = item,
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
                brush = Brush.linearGradient(
                    colors = listOf(DeepGreen, Color(0xFF025C46))
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.05f),
                radius = 160.dp.toPx(),
                center = Offset(size.width * 0.85f, -50.dp.toPx())
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.04f),
                radius = 100.dp.toPx(),
                center = Offset(-30.dp.toPx(), size.height * 0.8f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text  = "Budgeti",
                style = MaterialTheme.typography.headlineMedium,
                color = TextOnDark
            )
            Text(
                text  = "Postavi limite po kategoriji",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnDark.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun BudgetSummaryCard(items: List<BudgetItem>) {
    val budgetedItems     = items.filter { it.limitAmount != null }
    val totalBudget       = budgetedItems.sumOf { it.limitAmount ?: 0.0 }
    val totalSpent        = budgetedItems.sumOf { it.spentThisMonth }
    val overallProgress   = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceAtMost(1f) else 0f
    val overBudgetCount   = budgetedItems.count { it.isOverBudget }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = DeepGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text  = "Ukupni budget ovaj mjesec",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnDark.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text       = "€%.2f".format(totalSpent),
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = TextOnDark
                )
                Text(
                    text      = " / €%.2f".format(totalBudget),
                    style     = MaterialTheme.typography.titleMedium,
                    color     = TextOnDark.copy(alpha = 0.55f),
                    modifier  = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress    = { overallProgress },
                modifier    = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color       = if (overallProgress >= 1f) ErrorRed else AccentGold,
                trackColor  = TextOnDark.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text  = when {
                    overBudgetCount > 0   ->
                        "⚠️ $overBudgetCount ${if (overBudgetCount == 1) "kategorija" else "kategorije"} prešla limit"
                    budgetedItems.isEmpty() -> "💡 Dodiri kategoriju za postavljanje limita"
                    else                  -> "✅ Unutar budgeta"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    overBudgetCount > 0   -> Color(0xFFFFD700)
                    budgetedItems.isEmpty() -> AccentGold
                    else                  -> Color(0xFF86EFAC)
                }
            )
        }
    }
}

@Composable
fun BudgetCategoryCard(item: BudgetItem, onClick: () -> Unit) {
    val categoryColor = getCategoryColor(item.category.displayName)
    val progressColor = when {
        item.isOverBudget     -> ErrorRed
        item.progress >= 0.7f -> AccentGold
        else                  -> SuccessGreen
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick   = onClick
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle ikona kategorije
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = getCategoryEmoji(item.category.displayName),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = item.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDark
                    )
                    if (item.limitAmount != null) {
                        Text(
                            text  = "€%.2f / €%.2f".format(item.spentThisMonth, item.limitAmount),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (item.isOverBudget) ErrorRed else TextMuted
                        )
                    } else {
                        Text(
                            text  = "€%.2f  —  nema limita".format(item.spentThisMonth),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                if (item.limitAmount != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = { item.progress },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = progressColor,
                        trackColor = categoryColor.copy(alpha = 0.12f)
                    )
                    if (item.isOverBudget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "Prekoračen za €%.2f".format(item.spentThisMonth - item.limitAmount),
                            style = MaterialTheme.typography.labelSmall,
                            color = ErrorRed
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Dodiri za postavljanje limita",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.55f)
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
        title            = {
            Text(
                text  = "${getCategoryEmoji(category.displayName)} ${category.displayName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text  = "Postavi mjesečni limit za ovu kategoriju:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value           = inputText,
                    onValueChange   = { inputText = it },
                    label           = { Text("Limit (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepGreen,
                        focusedLabelColor  = DeepGreen
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { onConfirm(inputText.toDoubleOrNull()) },
                enabled  = isValid
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
                TextButton(onClick = onDismiss) { Text("Odustani") }
            }
        }
    )
}
