package com.phenom.gym.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely stores the mobile session (access + refresh token) and cached user info.
 * Uses EncryptedSharedPreferences (AES256) backed by Android Keystore.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "phenom_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun saveUserJson(json: String) {
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUserJson(): String? = prefs.getString(KEY_USER, null)

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    fun saveApiKeyMeta(name: String?, prefix: String?) {
        prefs.edit()
            .putString(KEY_API_KEY_NAME, name)
            .putString(KEY_API_KEY_PREFIX, prefix)
            .apply()
    }

    fun getApiKeyMeta(): Pair<String?, String?> =
        prefs.getString(KEY_API_KEY_NAME, null) to prefs.getString(KEY_API_KEY_PREFIX, null)

    /** Clear the locally-stored API key + metadata (called after unlink). */
    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_API_KEY_NAME)
            .remove(KEY_API_KEY_PREFIX)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank() || !getRefreshToken().isNullOrBlank()

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_USER = "user_json"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_KEY_NAME = "api_key_name"
        private const val KEY_API_KEY_PREFIX = "api_key_prefix"
    }
}
