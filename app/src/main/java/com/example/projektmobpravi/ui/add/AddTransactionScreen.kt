package com.example.projektmobpravi.ui.add

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import com.example.projektmobpravi.ui.home.getCategoryEmoji
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*

val supportedCurrencies = listOf("EUR", "USD", "GBP", "CHF", "HRK")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    scannedAmount: String? = null,
    transactionId: Int? = null,
    scannedAmountFromRoute: String = "",
    scannedNoteFromRoute: String = ""
) {
    val viewModel: AddTransactionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val savedScannedAmount = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("scannedAmount")
    val savedScannedNote = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("scannedNote")
    val newCategoryName = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("newCategoryName")
    val newCategoryEmoji = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("newCategoryEmoji")

    var amount by remember {
        mutableStateOf(savedScannedAmount ?: scannedAmountFromRoute.ifEmpty { scannedAmount } ?: "")
    }
    var note by remember {
        mutableStateOf(savedScannedNote ?: scannedNoteFromRoute.ifEmpty { null } ?: "")
    }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) viewModel.loadTransactionForEdit(transactionId)
    }
    LaunchedEffect(uiState.initialAmount) {
        if (uiState.isEditMode && uiState.initialAmount.isNotEmpty()) {
            amount = uiState.initialAmount
            note   = uiState.initialNote
        }
    }
    LaunchedEffect(savedScannedAmount) {
        savedScannedAmount?.let { if (it.isNotEmpty()) amount = it }
    }
    LaunchedEffect(savedScannedNote) {
        savedScannedNote?.let { if (it.isNotEmpty()) note = it }
    }
    LaunchedEffect(newCategoryName) {
        if (!newCategoryName.isNullOrEmpty()) {
            viewModel.addCustomCategory(newCategoryName, newCategoryEmoji ?: "📦")
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("newCategoryName")
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("newCategoryEmoji")
        }
    }
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        bottomBar      = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight
    ) { paddingValues ->
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
                        radius = 140.dp.toPx(),
                        center = Offset(size.width * 0.88f, -40.dp.toPx())
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
                        text  = if (uiState.isEditMode) "Uredi transakciju" else "Nova transakcija",
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
                // ── Iznos + Valuta ───────────────────────────────────
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
                            text  = "Iznos",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value           = amount,
                                onValueChange   = { amount = it; viewModel.clearError() },
                                label           = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier        = Modifier.weight(1f),
                                shape           = RoundedCornerShape(12.dp),
                                colors          = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = MintGreen,
                                    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                    focusedLabelColor    = MintGreen
                                ),
                                singleLine = true
                            )
                            Box {
                                Card(
                                    modifier = Modifier
                                        .clickable { showCurrencyDropdown = true }
                                        .width(90.dp)
                                        .height(56.dp),
                                    shape  = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = DeepGreen)
                                ) {
                                    Box(
                                        modifier         = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = uiState.selectedCurrency,
                                            style      = MaterialTheme.typography.titleMedium,
                                            color      = TextOnDark
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded          = showCurrencyDropdown,
                                    onDismissRequest  = { showCurrencyDropdown = false }
                                ) {
                                    supportedCurrencies.forEach { currency ->
                                        DropdownMenuItem(
                                            text    = { Text(currency, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = {
                                                viewModel.selectCurrency(currency)
                                                showCurrencyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (!uiState.isEditMode) {
                            OutlinedButton(
                                onClick  = { navController.navigate(Screen.Scan.route) },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MintGreen)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier           = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text  = "Skeniraj račun",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (!savedScannedAmount.isNullOrEmpty()) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SuccessGreen.copy(alpha = 0.09f))
                                    .padding(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Text(
                                    text  = "Iznos prepoznat s računa",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                // ── Kategorije ───────────────────────────────────────
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
                            text  = "Kategorija",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )

                        data class GridItem(
                            val name: String,
                            val emoji: String,
                            val isAddNew: Boolean = false,
                            val onClick: () -> Unit
                        )

                        val gridItems = buildList<GridItem> {
                            Category.values().forEach { cat ->
                                add(GridItem(cat.displayName, cat.emoji) {
                                    viewModel.selectCategory(cat)
                                })
                            }
                            uiState.customCategories.forEach { cat ->
                                add(GridItem(cat.name, cat.emoji) {
                                    viewModel.selectCustomCategory(cat.name, cat.emoji)
                                })
                            }
                            add(GridItem("Nova", "➕", isAddNew = true) {
                                navController.navigate(Screen.CreateCategory.route)
                            })
                        }

                        gridItems.chunked(4).forEach { rowItems ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isSelected = !item.isAddNew &&
                                        uiState.selectedCategoryName == item.name
                                    val isCustom = !item.isAddNew &&
                                        uiState.customCategories.any { it.name == item.name }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected   -> DeepGreen
                                                    item.isAddNew -> MintGreen.copy(alpha = 0.08f)
                                                    else          -> SurfaceLight
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 0.dp else 1.dp,
                                                color = when {
                                                    item.isAddNew -> MintGreen.copy(alpha = 0.4f)
                                                    else          -> TextMuted.copy(alpha = 0.18f)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .combinedClickable(
                                                onClick     = { item.onClick() },
                                                onLongClick = {
                                                    if (isCustom) categoryToDelete = item.name
                                                }
                                            )
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = item.emoji, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text  = item.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                isSelected    -> TextOnDark
                                                item.isAddNew -> MintGreen
                                                else          -> TextMuted
                                            },
                                            fontWeight = if (isSelected || item.isAddNew)
                                                FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // ── Napomena ─────────────────────────────────────────
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
                            text  = "Napomena",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        OutlinedTextField(
                            value         = note,
                            onValueChange = { note = it },
                            label         = { Text("npr. Konzum, benzinska...") },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MintGreen,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                                focusedLabelColor    = MintGreen
                            ),
                            maxLines = 3
                        )
                    }
                }

                uiState.errorMessage?.let { error ->
                    Text(
                        text     = error,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = ErrorRed,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Button(
                    onClick  = {
                        if (uiState.isEditMode) viewModel.updateTransaction(amount, note)
                        else viewModel.addTransaction(amount, note)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                    enabled  = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = TextOnDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text  = if (uiState.isEditMode) "Spremi promjene" else "Dodaj transakciju",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextOnDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    categoryToDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title            = { Text("Obriši kategoriju") },
            text             = { Text("Sigurno želiš obrisati kategoriju \"$name\"?") },
            confirmButton    = {
                TextButton(onClick = { viewModel.deleteCustomCategory(name); categoryToDelete = null }) {
                    Text("Obriši", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Odustani") }
            }
        )
    }
}
