package com.example.projektmobpravi.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
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

@Composable
fun StatsScreen(navController: NavHostController) {
    val viewModel: StatsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (uiState.exportSuccess || uiState.exportError) {
        LaunchedEffect(uiState.exportSuccess, uiState.exportError) {
            viewModel.clearExportState()
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
                        onExportClick  = { viewModel.exportTransactions(context) },
                        exportSuccess  = uiState.exportSuccess
                    )
                }
                item {
                    PeriodSelector(
                        selectedPeriod   = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
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

// ── Header ────────────────────────────────────────────

@Composable
fun StatsHeader(onExportClick: () -> Unit, exportSuccess: Boolean = false) {
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

// ── Period Selector ───────────────────────────────────

@Composable
fun PeriodSelector(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.values().forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) DeepGreen else SurfaceCard)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = TextMuted.copy(alpha = 0.18f),
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
                    color      = if (isSelected) TextOnDark else TextMuted
                )
            }
        }
    }
}

// ── Category Filter ───────────────────────────────────

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    if (categories.isEmpty()) return

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val allSelected = selectedCategory == null
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (allSelected) DeepGreen else SurfaceCard)
                .border(
                    width = if (allSelected) 0.dp else 1.dp,
                    color = TextMuted.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onCategorySelected(null) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "Sve",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = if (allSelected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (allSelected) TextOnDark else TextMuted
            )
        }

        categories.forEach { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) DeepGreen else SurfaceCard)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = TextMuted.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onCategorySelected(if (isSelected) null else category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = getCategoryEmoji(category), style = MaterialTheme.typography.labelSmall)
                    Text(
                        text       = category,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) TextOnDark else TextMuted
                    )
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
