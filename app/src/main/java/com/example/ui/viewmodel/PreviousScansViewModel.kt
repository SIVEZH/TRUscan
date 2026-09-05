package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScanEntity
import com.example.data.repository.ScanStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PreviousScansViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScanStorageRepository(application)
    
    private val _scans = MutableStateFlow<List<ScanEntity>>(emptyList())
    val scans: StateFlow<List<ScanEntity>> = _scans
    
    fun loadScans(userId: String) {
        viewModelScope.launch {
            repository.getScans(userId)
                .catch { e -> e.printStackTrace() }
                .collect { scanList ->
                    _scans.value = scanList
                }
        }
    }
}
