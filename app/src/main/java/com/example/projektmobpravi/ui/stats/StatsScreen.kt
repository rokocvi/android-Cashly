package com.example.projektmobpravi.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.home.getCategoryColor
import com.example.projektmobpravi.ui.home.getCategoryEmoji
import com.example.projektmobpravi.ui.theme.*
import androidx.compose.ui.viewinterop.AndroidView
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
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = SurfaceLight,
        snackbarHost = {}
    ) { paddingValues ->

        if (uiState.exportSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(top = paddingValues.calculateTopPadding()),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Exportano u Downloads!",
                        color = DeepGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

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
                // Header
                item {
                    StatsHeader(onExportClick = { viewModel.exportTransactions(context) })
                }

                // Period selector
                item {
                    PeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
                }

                // Filter po kategoriji
                item {
                    CategoryFilter(
                        categories = uiState.allCategories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }

                // Summary kartice
                item {
                    StatsSummaryRow(uiState = uiState)
                }

                // Kategorije
                item {
                    Text(
                        text = "Potrošnja po kategorijama",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                item {
                    CategoryStatsCard(categoryStats = uiState.categoryStats)
                }

                // Dnevni graf
                item {
                    Text(
                        text = "Zadnjih 7 dana",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                item {
                    DailyStatsCard(dailyStats = uiState.dailyStats)
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────

@Composable
fun StatsHeader(onExportClick: () -> Unit) {
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
                text = "Statistike 📊",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
            )
            Text(
                text = "Pregled tvoje potrošnje",
                fontSize = 13.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
        }
        OutlinedButton(
            onClick = onExportClick,
            modifier = Modifier.align(Alignment.CenterEnd),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextOnDark),
            border = BorderStroke(1.dp, TextOnDark.copy(alpha = 0.5f))
        ) {
            Text(text = "Export CSV", fontSize = 12.sp)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.values().forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) DeepGreen else SurfaceCard)
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextOnDark else TextMuted
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
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Sve" chip
        val allSelected = selectedCategory == null
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (allSelected) DeepGreen else SurfaceCard)
                .clickable { onCategorySelected(null) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sve",
                fontSize = 12.sp,
                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (allSelected) TextOnDark else TextMuted
            )
        }

        categories.forEach { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) DeepGreen else SurfaceCard)
                    .clickable { onCategorySelected(if (isSelected) null else category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = getCategoryEmoji(category), fontSize = 12.sp)
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextOnDark else TextMuted
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ukupno
        SummaryCard(
            modifier = Modifier.weight(1f),
            emoji = "💰",
            label = "Ukupno",
            value = "€%.2f".format(uiState.totalThisMonth),
            color = DeepGreen
        )

        // Prosjek po danu
        SummaryCard(
            modifier = Modifier.weight(1f),
            emoji = "📅",
            label = "Dnevno",
            value = "€%.2f".format(uiState.averagePerDay),
            color = MintGreen
        )

        // Prošli mjesec
        SummaryCard(
            modifier = Modifier.weight(1f),
            emoji = "📈",
            label = "Prošli mj.",
            value = "€%.2f".format(uiState.totalLastMonth),
            color = AccentGold
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

// ── Kategorije ────────────────────────────────────────

@Composable
fun CategoryStatsCard(categoryStats: List<CategoryStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (categoryStats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema podataka za ovaj period",
                        color = TextMuted,
                        fontSize = 14.sp
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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(stat.category),
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = stat.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "€%.2f".format(stat.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "%.1f%%".format(stat.percentage),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.1f))
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (dailyStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema podataka za ovaj period",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    factory = { context ->
                        BarChart(context).apply {
                            // Osnovne postavke
                            description.isEnabled = false
                            legend.isEnabled = false
                            setTouchEnabled(false)
                            setDrawGridBackground(false)
                            setDrawBorders(false)
                            animateY(800)

                            // X os
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity = 1f
                                textColor = android.graphics.Color.parseColor("#6B7280")
                                textSize = 10f
                            }

                            // Lijeva Y os
                            axisLeft.apply {
                                setDrawGridLines(true)
                                gridColor = android.graphics.Color.parseColor("#F0F0F0")
                                setDrawAxisLine(false)
                                textColor = android.graphics.Color.parseColor("#6B7280")
                                textSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "€${value.toInt()}"
                                    }
                                }
                            }

                            // Desna Y os — sakrij
                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        val entries = dailyStats.mapIndexed { index, stat ->
                            BarEntry(index.toFloat(), stat.amount.toFloat())
                        }

                        val dataSet = BarDataSet(entries, "").apply {
                            colors = listOf(
                                android.graphics.Color.parseColor("#2D6A4F"),
                                android.graphics.Color.parseColor("#1A3C34"),
                                android.graphics.Color.parseColor("#D4A853"),
                                android.graphics.Color.parseColor("#2D6A4F"),
                                android.graphics.Color.parseColor("#1A3C34"),
                                android.graphics.Color.parseColor("#D4A853"),
                                android.graphics.Color.parseColor("#2D6A4F")
                            )
                            valueTextColor = android.graphics.Color.parseColor("#1A1A1A")
                            valueTextSize = 9f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return "€${value.toInt()}"
                                }
                            }
                        }

                        // X os labele
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                            dailyStats.map { it.day }
                        )

                        chart.data = BarData(dataSet).apply {
                            barWidth = 0.6f
                        }
                        chart.invalidate()
                    }
                )
            }
        }
    }
}