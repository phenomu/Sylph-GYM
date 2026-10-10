package com.phenom.gym.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.BuildConfig
import com.phenom.gym.data.remote.BalanceResponse
import com.phenom.gym.data.remote.UserDto
import com.phenom.gym.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(
        context = app,
        baseUrl = BuildConfig.API_BASE_URL,
        googleClientId = BuildConfig.GOOGLE_CLIENT_ID
    )

    private val _user = MutableStateFlow<UserDto?>(repo.getUser())
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _balance = MutableStateFlow<BalanceResponse?>(null)
    val balance: StateFlow<BalanceResponse?> = _balance.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            // Fetch /auth/me (also refreshes cached user)
            repo.getMe().onSuccess { _user.value = it.user }
                .onFailure { _error.value = it.message }
            // Fetch balance only if an API key is linked
            if (repo.getApiKey().isNullOrBlank()) {
                _balance.value = null
            } else {
                repo.getBalance().onSuccess { _balance.value = it }
                    .onFailure { _balance.value = null }
            }
            _loading.value = false
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }
}
