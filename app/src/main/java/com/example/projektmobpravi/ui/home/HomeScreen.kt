package com.example.projektmobpravi.ui.home

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*
import com.example.projektmobpravi.util.ShakeDetector
import java.text.SimpleDateFormat
import java.util.Date
import android.hardware.Sensor
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Shake-to-scan
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    DisposableEffect(Unit) {
        val detector = ShakeDetector {
            scope.launch { navController.navigate(Screen.Scan.route) }
        }
        sensorManager.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(detector) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddTransaction.addRoute) },
                containerColor = DeepGreen,
                contentColor = TextOnDark,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj transakciju",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
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
                    HomeHeader(
                        username = uiState.username,
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                item {
                    TotalSpendingCard(total = uiState.totalThisMonth)
                }

                item {
                    CategorySummaryRow(categoryTotals = uiState.categoryTotals)
                }

                item {
                    Text(
                        text = "Zadnje transakcije",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyTransactionsCard()
                    }
                } else {
                    items(
                        items = uiState.transactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            onEdit = {
                                navController.navigate(
                                    Screen.AddTransaction.editRoute(transaction.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(username: String, onLogout: () -> Unit) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dobar dan, $username! 👋",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnDark
                )
                Text(
                    text = SimpleDateFormat(
                        "d. MMMM yyyy.", Locale("hr")
                    ).format(Date()),
                    fontSize = 13.sp,
                    color = TextOnDark.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Odjava",
                    tint = TextOnDark.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TotalSpendingCard(total: Double) {
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
                text = "Potrošnja ovaj mjesec",
                fontSize = 14.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "€ %.2f".format(total),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 Prati svaki trošak",
                fontSize = 12.sp,
                color = AccentGold
            )
        }
    }
}

@Composable
fun CategorySummaryRow(categoryTotals: Map<String, Double>) {
    val topCategories = categoryTotals.entries
        .sortedByDescending { it.value }
        .take(3)

    if (topCategories.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        topCategories.forEach { (category, amount) ->
            CategoryMiniCard(
                category = category,
                amount = amount,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun CategoryMiniCard(
    category: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    val emoji = getCategoryEmoji(category)
    val color = getCategoryColor(category)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "€%.0f".format(amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = category,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši transakciju") },
            text = { Text("Jesi li siguran da želiš obrisati ovu transakciju?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Obriši", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryEmoji(transaction.category),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.category },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = SimpleDateFormat("d. MMM yyyy.", Locale("hr"))
                        .format(Date(transaction.date)),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Text(
                text = "-€%.2f".format(transaction.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Uredi",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Obriši",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "💸", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Nema transakcija",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Dodaj svoju prvu transakciju!",
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Hrana" -> "🍔"
        "Prijevoz" -> "🚗"
        "Zabava" -> "🎬"
        "Kuća" -> "🏠"
        "Zdravlje" -> "💊"
        "Odijevanje" -> "👕"
        else -> "📦"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Hrana" -> CategoryFood
        "Prijevoz" -> CategoryTransport
        "Zabava" -> CategoryEntertainment
        "Kuća" -> CategoryHousing
        "Zdravlje" -> CategoryHealth
        "Odijevanje" -> CategoryClothing
        else -> CategoryOther
    }
}