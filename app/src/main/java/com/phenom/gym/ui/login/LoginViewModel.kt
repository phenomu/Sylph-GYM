package com.phenom.gym.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.BuildConfig
import com.phenom.gym.data.repo.AuthRepository
import com.phenom.gym.data.remote.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(
        context = app,
        baseUrl = BuildConfig.API_BASE_URL,
        googleClientId = BuildConfig.GOOGLE_CLIENT_ID
    )

    private val _ui = MutableStateFlow<LoginUi>(LoginUi.Idle)
    val ui: StateFlow<LoginUi> = _ui

    private val _alreadyLoggedIn = MutableStateFlow(repo.isLoggedIn())
    val alreadyLoggedIn: StateFlow<Boolean> = _alreadyLoggedIn

    fun signInWithGoogle(activityContext: Context) {
        _ui.value = LoginUi.Loading
        viewModelScope.launch {
            val res = repo.signInWithGoogle(activityContext)
            _ui.value = res.fold(
                onSuccess = { LoginUi.Success(it) },
                onFailure = { LoginUi.Error(it.message ?: "Login gagal") }
            )
        }
    }

    fun consumeSuccess() { _ui.value = LoginUi.Idle }
}

sealed interface LoginUi {
    data object Idle : LoginUi
    data object Loading : LoginUi
    data class Success(val data: AuthResponse) : LoginUi
    data class Error(val message: String) : LoginUi
}
