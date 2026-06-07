package com.example.projektmobpravi.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*
import com.example.projektmobpravi.ui.theme.LocalTheme
import com.example.projektmobpravi.util.ShakeDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavHostController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    var showShakeTip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        showShakeTip = true
        delay(3000)
        showShakeTip = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { navController.navigate(Screen.AddTransaction.addRoute) },
                containerColor   = MaterialTheme.colorScheme.primary,
                contentColor     = TextOnDark,
                shape            = RoundedCornerShape(16.dp),
                modifier         = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Dodaj transakciju",
                    modifier           = Modifier.size(24.dp)
                )
            }
        },
        bottomBar      = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(
                modifier        = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    HomeHeader(
                        username = uiState.username,
                        onLogout = { viewModel.logout() }
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = uiState.isOffline,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentGold.copy(alpha = 0.12f))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint               = AccentGold,
                                modifier           = Modifier.size(16.dp)
                            )
                            Text(
                                text  = "Nema internetske veze — prikazuju se lokalni podaci",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentGold
                            )
                        }
                    }
                }

                item { TotalSpendingCard(total = uiState.totalThisMonth) }

                item { CategorySummaryRow(categoryTotals = uiState.categoryTotals) }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Zadnje transakcije",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        Text(
                            text  = "${uiState.transactions.size} stavki",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item { EmptyTransactionsCard() }
                } else {
                    items(
                        items = uiState.transactions,
                        key   = { it.id }
                    ) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onDelete    = { viewModel.deleteTransaction(transaction) },
                            onEdit      = {
                                navController.navigate(
                                    Screen.AddTransaction.editRoute(transaction.id)
                                )
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible  = showShakeTip,
            enter    = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit     = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = paddingValues.calculateBottomPadding() + 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DeepGreen.copy(alpha = 0.92f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📳", fontSize = 15.sp)
                Text(
                    text  = "Protresite mobitel za skeniranje računa",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
        } // outer Box
    }
}

@Composable
fun HomeHeader(username: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DeepGreen, BrandEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color  = Color.White.copy(alpha = 0.06f),
                radius = 180.dp.toPx(),
                center = Offset(size.width * 0.88f, -55.dp.toPx())
            )
            drawCircle(
                color  = Color.White.copy(alpha = 0.04f),
                radius = 110.dp.toPx(),
                center = Offset(-35.dp.toPx(), size.height * 0.85f)
            )
            // Suptilni zlatni glow na dnu
            drawCircle(
                color  = Color(0xFFF79009).copy(alpha = 0.07f),
                radius = 90.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 1.3f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar s inicijalima
                    Box(
                        modifier         = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = username.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextOnDark
                        )
                    }

                    Column {
                        Text(
                            text  = "Dobar dan,",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnDark.copy(alpha = 0.65f)
                        )
                        Text(
                            text  = username,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextOnDark
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val theme = LocalTheme.current

                    // Tema toggle (sunce / mjesec)
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { theme.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = if (theme.isDark)
                                Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (theme.isDark) "Svjetla tema" else "Tamna tema",
                            tint               = TextOnDark.copy(alpha = 0.9f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }

                    // Logout gumb
                    Box(
                        modifier         = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onLogout() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Odjava",
                            tint               = TextOnDark.copy(alpha = 0.9f),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text  = SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr")).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = TextOnDark.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
fun TotalSpendingCard(total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape   = RoundedCornerShape(20.dp),
        colors  = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Potrošnja ovaj mjesec",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text       = "€ %.2f".format(total),
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Text(
                        text  = "Aktivno praćenje",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Box(
                modifier         = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySummaryRow(categoryTotals: Map<String, Double>) {
    val topCategories = categoryTotals.entries
        .sortedByDescending { it.value }
        .take(3)

    if (topCategories.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text     = "Top kategorije",
            style    = MaterialTheme.typography.titleMedium,
            color    = TextDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            topCategories.forEach { (category, amount) ->
                CategoryMiniCard(
                    category = category,
                    amount   = amount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
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
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = "€%.0f".format(amount),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = TextDark
            )
            Text(
                text     = category,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            title            = { Text("Obriši transakciju") },
            text             = { Text("Jesi li siguran da želiš obrisati ovu transakciju?") },
            confirmButton    = {
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
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Obojana lijeva traka po boji kategorije
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(getCategoryColor(transaction.category))
            )
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
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = getCategoryEmoji(transaction.category),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Naziv i datum
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = transaction.note.ifBlank { transaction.category },
                    style    = MaterialTheme.typography.titleMedium,
                    color    = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(getCategoryColor(transaction.category).copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text  = transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = getCategoryColor(transaction.category)
                        )
                    }
                    Text(
                        text  = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text  = SimpleDateFormat("d. MMM", Locale("hr"))
                            .format(Date(transaction.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Iznos i akcije
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = "-€%.2f".format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = ErrorRed
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = "Uredi",
                        tint               = TextMuted.copy(alpha = 0.45f),
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { onEdit() }
                    )
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Obriši",
                        tint               = TextMuted.copy(alpha = 0.45f),
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { showDeleteDialog = true }
                    )
                }
            }
            }
        }
    }
}

@Composable
fun EmptyTransactionsCard() {
    Column(
        modifier             = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💸", fontSize = 34.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text  = "Nema transakcija",
            style = MaterialTheme.typography.titleMedium,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = "Dodaj svoju prvu transakciju\nklikom na gumb + ispod",
            style     = MaterialTheme.typography.bodySmall,
            color     = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category) {
        "Hrana"      -> "🍔"
        "Prijevoz"   -> "🚗"
        "Zabava"     -> "🎬"
        "Kuća"       -> "🏠"
        "Zdravlje"   -> "💊"
        "Odijevanje" -> "👕"
        else         -> "📦"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Hrana"      -> CategoryFood
        "Prijevoz"   -> CategoryTransport
        "Zabava"     -> CategoryEntertainment
        "Kuća"       -> CategoryHousing
        "Zdravlje"   -> CategoryHealth
        "Odijevanje" -> CategoryClothing
        else         -> CategoryOther
    }
}
