package com.phenom.gym.ui.apikeys

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.BuildConfig
import com.phenom.gym.data.remote.ApiException
import com.phenom.gym.data.remote.ApiKeyDto
import com.phenom.gym.data.remote.KeyValidateResponse
import com.phenom.gym.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ApiKeysViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(
        context = app,
        baseUrl = BuildConfig.API_BASE_URL,
        googleClientId = BuildConfig.GOOGLE_CLIENT_ID
    )

    private val _keys = MutableStateFlow<List<ApiKeyDto>>(emptyList())
    val keys: StateFlow<List<ApiKeyDto>> = _keys.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _validateResult = MutableStateFlow<KeyValidateResponse?>(null)
    val validateResult: StateFlow<KeyValidateResponse?> = _validateResult.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refresh() }

    fun setInput(v: String) { _input.value = v }

    fun refresh() {
        _busy.value = true
        viewModelScope.launch {
            repo.listKeys().onSuccess { _keys.value = it }
                .onFailure { _message.value = it.message }
            _busy.value = false
        }
    }

    fun validate() {
        val key = _input.value.trim()
        if (key.isEmpty()) { _message.value = "Masukkan API key"; return }
        _busy.value = true
        _message.value = null
        _validateResult.value = null
        viewModelScope.launch {
            repo.validateKey(key).onSuccess { res ->
                _validateResult.value = res
                // Save key locally if already linked to this account (for AI chat)
                if (res.valid && res.already_linked == true) {
                    repo.saveKeyForChat(key)
                }
                if (!res.valid) _message.value = res.message ?: "Key tidak valid"
            }.onFailure { _message.value = it.message }
            _busy.value = false
        }
    }

    fun link() {
        val key = _input.value.trim()
        if (key.isEmpty()) { _message.value = "Masukkan API key"; return }
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            repo.linkKey(key).onSuccess {
                _message.value = "API key berhasil dihubungkan ✓"
                _input.value = ""
                _validateResult.value = null
                refresh()
            }.onFailure { e ->
                _message.value = when (e) {
                    is ApiException -> if (e.code == 409) "Key sudah di-link email lain" else e.errorBody
                    else -> e.message
                }
            }
            _busy.value = false
        }
    }

    fun unlink(id: Long) {
        _busy.value = true
        viewModelScope.launch {
            repo.unlinkKey(id).onSuccess {
                _message.value = "Key di-unlink"
                refresh()
            }.onFailure { _message.value = it.message }
            _busy.value = false
        }
    }
}
