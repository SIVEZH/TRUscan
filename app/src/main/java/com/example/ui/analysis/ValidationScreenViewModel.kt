package com.example.ui.analysis

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ScanStorageRepository
import com.example.data.repository.analysis.ProductAnalysisRepository
import com.example.domain.model.GeminiExtraction
import com.example.domain.model.ValidationReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val report: ValidationReport, val extraction: GeminiExtraction, val scanId: String) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class ValidationScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductAnalysisRepository(application)
    private val storageRepository = ScanStorageRepository(application)
    
    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val state: StateFlow<AnalysisState> = _state

    fun analyzeProduct(frontUri: Uri, backUri: Uri, userId: String) {
        _state.value = AnalysisState.Loading
        viewModelScope.launch {
            val result = repository.analyzeProduct(frontUri, backUri)
            if (result.isSuccess) {
                val (report, extraction) = result.getOrNull()!!
                val saveResult = storageRepository.saveScan(userId, frontUri.toString(), backUri.toString(), report)
                if (saveResult.isSuccess) {
                    val scanId = saveResult.getOrNull()!!
                    _state.value = AnalysisState.Success(report, extraction, scanId)
                } else {
                    _state.value = AnalysisState.Error(saveResult.exceptionOrNull()?.message ?: "Failed to save scan result.")
                }
            } else {
                _state.value = AnalysisState.Error(result.exceptionOrNull()?.message ?: "An unknown error occurred.")
            }
        }
    }
}
