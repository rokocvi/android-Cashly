package com.example.projektmobpravi.ui.add

import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.projektmobpravi.domain.model.Category
import com.example.projektmobpravi.ui.components.BottomNavigationBar
import com.example.projektmobpravi.ui.home.getCategoryEmoji
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.*
import com.example.projektmobpravi.util.VoiceInputHelper
import com.example.projektmobpravi.util.VoiceParseResult
import com.example.projektmobpravi.util.VoiceParser

val supportedCurrencies = listOf("EUR", "USD", "GBP", "CHF", "HRK")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    scannedAmount: String? = null,
    transactionId: Int? = null,
    scannedAmountFromRoute: String = "",
    scannedNoteFromRoute: String = "",
    scannedCategoryFromRoute: String = "",
    autoVoice: Boolean = false
) {
    val viewModel: AddTransactionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val strings = LocalStrings.current

    val savedScannedAmount   = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("scannedAmount")
    val savedScannedNote     = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("scannedNote")
    val savedScannedCategory = navController.currentBackStackEntry
        ?.savedStateHandle?.get<String>("scannedCategory")
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
    var categoryToDelete    by remember { mutableStateOf<String?>(null) }

    // ── Voice input state ────────────────────────────────────────────
    var isListening   by remember { mutableStateOf(false) }
    var voiceLang     by remember { mutableStateOf("hr-HR") }
    var voiceResult   by remember { mutableStateOf<VoiceParseResult?>(null) }
    var voiceError    by remember { mutableStateOf<String?>(null) }
    var showVoiceHelp by remember { mutableStateOf(false) }

    val voiceHelper = remember(context) { VoiceInputHelper(context) }
    DisposableEffect(voiceHelper) { onDispose { voiceHelper.destroy() } }

    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            voiceHelper.startListening(
                languageTag = voiceLang,
                onResult    = { text ->
                    isListening = false
                    if (text.isNotEmpty()) {
                        voiceResult = VoiceParser.parse(text, uiState.customCategories)
                        if (voiceResult == null) voiceError = "${strings.voiceNotRecognizedPrefix} \"$text\""
                    } else {
                        voiceError = strings.voiceNoSpeech
                    }
                },
                onError = { msg -> isListening = false; voiceError = msg }
            )
        } else {
            voiceError = strings.voiceNoPerm
        }
    }

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
    LaunchedEffect(savedScannedCategory ?: scannedCategoryFromRoute) {
        val catName = (savedScannedCategory?.takeIf { it.isNotEmpty() }
            ?: scannedCategoryFromRoute.takeIf { it.isNotEmpty() }) ?: return@LaunchedEffect
        Category.values().firstOrNull { it.displayName == catName }
            ?.let { viewModel.selectCategory(it) }
    }
    LaunchedEffect(autoVoice) {
        if (!autoVoice) return@LaunchedEffect
        delay(350)
        voiceResult = null
        voiceError  = null
        val perm = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        if (perm == PackageManager.PERMISSION_GRANTED) {
            isListening = true
            voiceHelper.startListening(
                languageTag = voiceLang,
                onResult    = { text ->
                    isListening = false
                    if (text.isNotEmpty()) {
                        voiceResult = VoiceParser.parse(text, uiState.customCategories)
                        if (voiceResult == null) voiceError = "${strings.voiceNotRecognizedPrefix} \"$text\""
                    } else {
                        voiceError = strings.voiceNoSpeech
                    }
                },
                onError = { msg -> isListening = false; voiceError = msg }
            )
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
                            colors = listOf(DeepGreen, BrandEnd)
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
                        text  = if (uiState.isEditMode) strings.editTransaction else strings.addNewTransaction,
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
                            text  = strings.amountLabel,
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
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
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
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick  = { navController.navigate(Screen.Scan.route) },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MintGreen)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.scanButton, style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = {
                                        voiceResult = null
                                        voiceError  = null
                                        val perm = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                        if (perm == PackageManager.PERMISSION_GRANTED) {
                                            isListening = true
                                            voiceHelper.startListening(
                                                languageTag = voiceLang,
                                                onResult    = { text ->
                                                    isListening = false
                                                    if (text.isNotEmpty()) {
                                                        voiceResult = VoiceParser.parse(text, uiState.customCategories)
                                                        if (voiceResult == null) voiceError = "${strings.voiceNotRecognizedPrefix} \"$text\""
                                                    } else {
                                                        voiceError = strings.voiceNoSpeech
                                                    }
                                                },
                                                onError = { msg -> isListening = false; voiceError = msg }
                                            )
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(12.dp),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor   = MintGreen,
                                        containerColor = if (isListening)
                                            MintGreen.copy(alpha = pulseAlpha * 0.12f)
                                        else Color.Transparent
                                    )
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint               = MintGreen.copy(alpha = if (isListening) pulseAlpha else 1f),
                                        modifier           = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text  = if (isListening) strings.voiceListening else strings.voiceButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MintGreen.copy(alpha = if (isListening) pulseAlpha else 1f)
                                    )
                                }
                            }

                            // Language toggle + help
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Info,
                                    contentDescription = "Upute za glasovni unos",
                                    tint               = TextMuted.copy(alpha = 0.55f),
                                    modifier           = Modifier
                                        .size(16.dp)
                                        .clickable { showVoiceHelp = true }
                                )
                                Text(
                                    text  = strings.voiceLangLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                listOf("hr-HR" to "HR", "en-US" to "EN").forEach { (code, label) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (voiceLang == code) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = if (voiceLang == code) MaterialTheme.colorScheme.primary else TextMuted.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { voiceLang = code }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = label,
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = if (voiceLang == code) TextOnDark else TextMuted,
                                            fontWeight = if (voiceLang == code) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Voice result preview
                            voiceResult?.let { result ->
                                VoiceResultBanner(
                                    result    = result,
                                    onDismiss = { voiceResult = null },
                                    onConfirm = {
                                        amount = String.format(java.util.Locale.US, "%.2f", result.amount)
                                        val builtin = Category.values().firstOrNull { it.displayName == result.categoryName }
                                        if (builtin != null) viewModel.selectCategory(builtin)
                                        else viewModel.selectCustomCategory(result.categoryName, result.categoryEmoji)
                                        voiceResult = null
                                    }
                                )
                            }

                            // Voice error
                            voiceError?.let { err ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ErrorRed.copy(alpha = 0.07f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text     = err,
                                        style    = MaterialTheme.typography.labelMedium,
                                        color    = ErrorRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(ErrorRed.copy(alpha = 0.10f))
                                            .clickable { voiceError = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                                    }
                                }
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
                                    text  = strings.amountFromScan,
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
                            text  = strings.categoryLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )

                        data class GridItem(
                            val key: String,
                            val displayName: String,
                            val emoji: String,
                            val isAddNew: Boolean = false,
                            val onClick: () -> Unit
                        )

                        val gridItems = buildList<GridItem> {
                            Category.values().forEach { cat ->
                                add(GridItem(cat.displayName, strings.categoryDisplayName(cat.displayName), cat.emoji) {
                                    viewModel.selectCategory(cat)
                                })
                            }
                            uiState.customCategories.forEach { cat ->
                                add(GridItem(cat.name, cat.name, cat.emoji) {
                                    viewModel.selectCustomCategory(cat.name, cat.emoji)
                                })
                            }
                            add(GridItem("", strings.addNewCategoryLabel, "➕", isAddNew = true) {
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
                                        uiState.selectedCategoryName == item.key
                                    val isCustom = !item.isAddNew &&
                                        uiState.customCategories.any { it.name == item.key }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected   -> MaterialTheme.colorScheme.primary
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
                                                    if (isCustom) categoryToDelete = item.key
                                                }
                                            )
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = item.emoji, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text  = item.displayName,
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
                            text  = strings.noteLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        OutlinedTextField(
                            value         = note,
                            onValueChange = { note = it },
                            label         = { Text(strings.notePlaceholder) },
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
                        // Ako banner čeka potvrdu, primijeni glasovni rezultat prije submita
                        voiceResult?.let { result ->
                            amount = String.format(java.util.Locale.US, "%.2f", result.amount)
                            val builtin = Category.values().firstOrNull { it.displayName == result.categoryName }
                            if (builtin != null) viewModel.selectCategory(builtin)
                            else viewModel.selectCustomCategory(result.categoryName, result.categoryEmoji)
                            voiceResult = null
                        }
                        if (uiState.isEditMode) viewModel.updateTransaction(amount, note)
                        else viewModel.addTransaction(amount, note)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                            text  = if (uiState.isEditMode) strings.saveChanges else strings.addTransactionButton,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextOnDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showVoiceHelp) {
        AlertDialog(
            onDismissRequest = { showVoiceHelp = false },
            shape            = RoundedCornerShape(20.dp),
            icon             = {
                Icon(Icons.Default.Mic, null, tint = MintGreen, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text  = strings.voiceHelpTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text  = strings.voiceHelpDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    VoiceHelpSection(
                        title   = strings.voiceExamplesCroatian,
                        examples = listOf(
                            "\"dvadeset pet eura, hrana\"",
                            "\"pedeset prijevoz\"",
                            "\"sto dvadeset zabava\"",
                            "\"petnaest zdravlje\""
                        )
                    )
                    VoiceHelpSection(
                        title    = strings.voiceExamplesEnglish,
                        examples = listOf(
                            "\"twenty five euros, food\"",
                            "\"fifty transport\"",
                            "\"fifteen health\""
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGold.copy(alpha = 0.08f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text  = strings.voiceDecimalTip,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDark
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceHelp = false }) {
                    Text(strings.voiceHelpOk, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    categoryToDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title            = { Text(strings.deleteCategoryTitle) },
            text             = { Text(strings.deleteCategoryMessage.format(name)) },
            confirmButton    = {
                TextButton(onClick = { viewModel.deleteCustomCategory(name); categoryToDelete = null }) {
                    Text(strings.delete, color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun VoiceHelpSection(title: String, examples: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        examples.forEach { example ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Text("•", style = MaterialTheme.typography.labelSmall, color = MintGreen)
                Text(example, style = MaterialTheme.typography.labelSmall, color = TextDark)
            }
        }
    }
}

@Composable
private fun VoiceResultBanner(
    result: VoiceParseResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessGreen.copy(alpha = 0.08f))
            .border(1.dp, SuccessGreen.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text  = strings.voiceRecognizedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = "€%.2f".format(result.amount),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text("—", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(
                    text       = "${result.categoryEmoji} ${result.categoryName}",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text  = "\"${result.rawText}\"",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.10f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.15f))
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}
