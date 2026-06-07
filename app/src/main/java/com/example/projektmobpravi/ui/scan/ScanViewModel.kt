package com.example.projektmobpravi.ui.scan

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.projektmobpravi.util.OcrParser
import com.example.projektmobpravi.util.OcrResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val ocrResult: OcrResult? = null,
    val errorMessage: String? = null,
    val hasPermission: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    fun onImageCaptured(text: String) {
        _uiState.value = _uiState.value.copy(isScanning = true)

        Log.d("OCR_RAW", "=== RAW OCR TEXT ===")
        text.lines().forEachIndexed { i, line -> Log.d("OCR_RAW", "$i: $line") }
        Log.d("OCR_RAW", "=== END ===")

        val result = OcrParser.parse(text)

        Log.d("OCR_RAW", "Parsed amount: ${result.amount} | store: ${result.storeName}")

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            ocrResult = result
        )
    }

    fun onScanError(message: String) {
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            errorMessage = message
        )
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            ocrResult = null,
            errorMessage = null
        )
    }
}