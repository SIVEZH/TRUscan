package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ValidationRepository
import com.example.domain.model.ValidationState
import com.example.domain.model.ValidationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ValidationRepository(application)
    
    private val _frontImageUri = MutableStateFlow<Uri?>(null)
    val frontImageUri: StateFlow<Uri?> = _frontImageUri

    private val _backImageUri = MutableStateFlow<Uri?>(null)
    val backImageUri: StateFlow<Uri?> = _backImageUri

    private val _frontValidationState = MutableStateFlow(ValidationState())
    val frontValidationState: StateFlow<ValidationState> = _frontValidationState

    private val _backValidationState = MutableStateFlow(ValidationState())
    val backValidationState: StateFlow<ValidationState> = _backValidationState

    fun setFrontImage(uri: Uri?) {
        _frontImageUri.value = uri
        if (uri != null) {
            _frontValidationState.value = ValidationState(ValidationStatus.VALIDATING, "Checking image...")
            viewModelScope.launch {
                val result = repository.validateImage(uri)
                _frontValidationState.value = result
            }
        } else {
            _frontValidationState.value = ValidationState(ValidationStatus.EMPTY)
        }
    }

    fun setBackImage(uri: Uri?) {
        _backImageUri.value = uri
        if (uri != null) {
            _backValidationState.value = ValidationState(ValidationStatus.VALIDATING, "Checking image...")
            viewModelScope.launch {
                val result = repository.validateImage(uri)
                _backValidationState.value = result
            }
        } else {
            _backValidationState.value = ValidationState(ValidationStatus.EMPTY)
        }
    }
    
    fun clearImages() {
        _frontImageUri.value = null
        _backImageUri.value = null
        _frontValidationState.value = ValidationState(ValidationStatus.EMPTY)
        _backValidationState.value = ValidationState(ValidationStatus.EMPTY)
    }
}

