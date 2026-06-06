package com.example.projektmobpravi.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.home.getCategoryColor
import com.example.projektmobpravi.ui.home.getCategoryEmoji
import com.example.projektmobpravi.ui.theme.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavHostController) {
    val viewModel: StatsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate
    )

    if (uiState.exportSuccess || uiState.exportError) {
        LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
            viewModel.clearExportState()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text("Odaberi", color = DeepGreen, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Odustani", color = TextMuted, style = MaterialTheme.typography.labelLarge)
                }
            },
            shape  = RoundedCornerShape(24.dp),
            colors = DatePickerDefaults.colors(
                containerColor            = SurfaceCard,
                titleContentColor         = TextDark,
                headlineContentColor      = DeepGreen,
                weekdayContentColor       = TextMuted,
                selectedDayContainerColor = DeepGreen,
                selectedDayContentColor   = TextOnDark,
                todayContentColor         = DeepGreen,
                todayDateBorderColor      = DeepGreen
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        bottomBar      = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight,
        snackbarHost   = {}
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
                item {
                    StatsHeader(
                        onExportClick   = { viewModel.exportTransactions(context) },
                        onCalendarClick = { showDatePicker = true },
                        exportSuccess   = uiState.exportSuccess
                    )
                }
                item {
                    PeriodSelector(
                        selectedPeriod   = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) },
                        dimmed           = uiState.selectedDate != null
                    )
                }
                if (uiState.selectedDate != null) {
                    item {
                        DateFilterChip(
                            selectedDateMillis = uiState.selectedDate!!,
                            onClear            = { viewModel.selectDate(null) }
                        )
                    }
                }
                item {
                    CategoryFilter(
                        categories         = uiState.allCategories,
                        selectedCategory   = uiState.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }
                item { StatsSummaryRow(uiState = uiState) }
                item {
                    Text(
                        text     = "Potrošnja po kategorijama",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                item { CategoryStatsCard(categoryStats = uiState.categoryStats) }
                if (uiState.selectedDate == null) {
                    item {
                        Text(
                            text     = "Zadnjih 7 dana",
                            style    = MaterialTheme.typography.titleMedium,
                            color    = TextDark,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    item { DailyStatsCard(dailyStats = uiState.dailyStats) }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────

@Composable
fun StatsHeader(
    onExportClick: () -> Unit,
    onCalendarClick: () -> Unit,
    exportSuccess: Boolean = false
) {
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

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Statistike",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextOnDark
                )
                Text(
                    text  = if (exportSuccess) "✓ Exportano u Downloads" else "Pregled tvoje potrošnje",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exportSuccess) AccentGold else TextOnDark.copy(alpha = 0.65f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { onCalendarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.DateRange,
                        contentDescription = "Filtriraj po danu",
                        tint               = TextOnDark,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable { onExportClick() }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Download,
                            contentDescription = null,
                            tint               = TextOnDark,
                            modifier           = Modifier.size(16.dp)
                        )
                        Text(
                            text  = "CSV",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextOnDark
                        )
                    }
                }
            }
        }
    }
}

// ── Period Selector ───────────────────────────────────

@Composable
fun PeriodSelector(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit,
    dimmed: Boolean = false
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.values().forEach { period ->
            val isSelected = selectedPeriod == period && !dimmed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) DeepGreen
                        else SurfaceCard.copy(alpha = if (dimmed) 0.5f else 1f)
                    )
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = TextMuted.copy(alpha = if (dimmed) 0.10f else 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = period.displayName,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) TextOnDark
                                 else TextMuted.copy(alpha = if (dimmed) 0.40f else 1f)
                )
            }
        }
    }
}

// ── Date Filter Chip ──────────────────────────────────

@Composable
fun DateFilterChip(selectedDateMillis: Long, onClear: () -> Unit) {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCal.timeInMillis = selectedDateMillis
    val label = "${utcCal.get(Calendar.DAY_OF_MONTH)}. " +
                "${utcCal.get(Calendar.MONTH) + 1}. " +
                "${utcCal.get(Calendar.YEAR)}."

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PrimaryContainer)
            .border(1.dp, DeepGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.CalendarToday,
                contentDescription = null,
                tint               = DeepGreen,
                modifier           = Modifier.size(16.dp)
            )
            Text(
                text       = "Prikazano: $label",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = DeepGreen
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(DeepGreen.copy(alpha = 0.12f))
                .clickable { onClear() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Ukloni filter dana",
                tint               = DeepGreen,
                modifier           = Modifier.size(13.dp)
            )
        }
    }
}

// ── Category Filter ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    if (categories.isEmpty()) return

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Trigger button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(
                width = 1.dp,
                color = if (selectedCategory != null) DeepGreen.copy(alpha = 0.50f)
                        else TextMuted.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { showSheet = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.FilterList,
                contentDescription = null,
                tint               = if (selectedCategory != null) DeepGreen else TextMuted,
                modifier           = Modifier.size(18.dp)
            )
            Text(
                text       = if (selectedCategory != null)
                                 "${getCategoryEmoji(selectedCategory)} $selectedCategory"
                             else "Sve kategorije",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedCategory != null) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selectedCategory != null) DeepGreen else TextMuted
            )
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selectedCategory != null) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(DeepGreen.copy(alpha = 0.10f))
                        .clickable { onCategorySelected(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Ukloni filter",
                        tint               = DeepGreen,
                        modifier           = Modifier.size(12.dp)
                    )
                }
            }
            Icon(
                imageVector        = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint               = TextMuted,
                modifier           = Modifier.size(20.dp)
            )
        }
    }

    // Bottom sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor   = SurfaceLight,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextMuted.copy(alpha = 0.25f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text  = "Filtriraj kategorije",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        Text(
                            text  = "${categories.size} kategorija dostupno",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(TextMuted.copy(alpha = 0.08f))
                            .clickable { showSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Zatvori",
                            tint               = TextMuted,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.10f))

                val allSelected = selectedCategory == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (allSelected) PrimaryContainer else Color.Transparent)
                        .clickable {
                            onCategorySelected(null)
                            showSheet = false
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepGreen.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.FilterList,
                                contentDescription = null,
                                tint               = DeepGreen,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text       = "Sve kategorije",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (allSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (allSelected) DeepGreen else TextDark
                        )
                    }
                    if (allSelected) {
                        Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = null,
                            tint               = DeepGreen,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.10f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val catColor   = getCategoryColor(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) PrimaryContainer else Color.Transparent)
                                .clickable {
                                    onCategorySelected(if (isSelected) null else category)
                                    showSheet = false
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(catColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text  = getCategoryEmoji(category),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text       = category,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (isSelected) DeepGreen else TextDark
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector        = Icons.Default.Check,
                                    contentDescription = null,
                                    tint               = DeepGreen,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Summary Row ───────────────────────────────────────

@Composable
fun StatsSummaryRow(uiState: StatsUiState) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.AccountBalanceWallet,
            label    = "Ukupno",
            value    = "€%.2f".format(uiState.totalThisMonth),
            color    = DeepGreen
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.CalendarToday,
            label    = "Dnevno",
            value    = "€%.2f".format(uiState.averagePerDay),
            color    = MintGreen
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon     = Icons.Default.TrendingUp,
            label    = "Prošli mj.",
            value    = "€%.2f".format(uiState.totalLastMonth),
            color    = AccentGold
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = color,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = color
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

// ── Kategorije ────────────────────────────────────────

@Composable
fun CategoryStatsCard(categoryStats: List<CategoryStat>) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (categoryStats.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Nema podataka za ovaj period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                categoryStats.forEach { stat ->
                    CategoryStatRow(stat = stat)
                }
            }
        }
    }
}

@Composable
fun CategoryStatRow(stat: CategoryStat) {
    val color = getCategoryColor(stat.category)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getCategoryEmoji(stat.category), style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text  = stat.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "€%.2f".format(stat.amount),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                )
                Text(
                    text  = "%.1f%%".format(stat.percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stat.percentage / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ── Dnevni Graf ───────────────────────────────────────

@Composable
fun DailyStatsCard(dailyStats: List<DailyStat>) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (dailyStats.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Nema podataka za ovaj period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    factory  = { context ->
                        BarChart(context).apply {
                            description.isEnabled  = false
                            legend.isEnabled       = false
                            setTouchEnabled(false)
                            setDrawGridBackground(false)
                            setDrawBorders(false)
                            animateY(600)

                            xAxis.apply {
                                position          = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity       = 1f
                                textColor         = android.graphics.Color.parseColor("#667085")
                                textSize          = 10f
                            }
                            axisLeft.apply {
                                setDrawGridLines(true)
                                gridColor         = android.graphics.Color.parseColor("#F1F5F9")
                                setDrawAxisLine(false)
                                textColor         = android.graphics.Color.parseColor("#667085")
                                textSize          = 10f
                                valueFormatter    = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float) = "€${value.toInt()}"
                                }
                            }
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val entries = dailyStats.mapIndexed { i, stat ->
                            BarEntry(i.toFloat(), stat.amount.toFloat())
                        }
                        val dataSet = BarDataSet(entries, "").apply {
                            colors = listOf(
                                android.graphics.Color.parseColor("#027A48"),
                                android.graphics.Color.parseColor("#014737"),
                                android.graphics.Color.parseColor("#F79009"),
                                android.graphics.Color.parseColor("#027A48"),
                                android.graphics.Color.parseColor("#014737"),
                                android.graphics.Color.parseColor("#F79009"),
                                android.graphics.Color.parseColor("#027A48")
                            )
                            valueTextColor = android.graphics.Color.parseColor("#101828")
                            valueTextSize  = 9f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float) =
                                    if (value > 0) "€${value.toInt()}" else ""
                            }
                        }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dailyStats.map { it.day })
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.invalidate()
                    }
                )
            }
        }
    }
}
