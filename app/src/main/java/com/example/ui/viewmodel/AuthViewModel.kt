package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserEntity
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserEntity) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        viewModelScope.launch {
            repository.seedDatabase()
            checkSession()
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            if (user != null) {
                _authState.value = AuthState.Success(user)
            }
        }
    }

    fun login(loginId: String, passwordRaw: String, expectedRole: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(loginId, passwordRaw)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null && user.role == expectedRole) {
                    _authState.value = AuthState.Success(user)
                } else {
                    // if it's the wrong role, clear session and show error
                    repository.logout()
                    _authState.value = AuthState.Error("Invalid email or password.")
                }
            } else {
                _authState.value = AuthState.Error("Invalid email or password.")
            }
        }
    }

    fun register(name: String, email: String, passwordRaw: String, role: String = "USER") {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(name, email, passwordRaw, role)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    _authState.value = AuthState.Success(user)
                }
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
        }
    }
    
    fun resetError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
