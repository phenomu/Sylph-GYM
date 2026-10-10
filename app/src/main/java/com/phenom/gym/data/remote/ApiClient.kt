package com.phenom.gym.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal OkHttp-based API client for the mobile backend at token.ate.my.id.
 * Handles JWT auth header injection + automatic refresh on 401.
 *
 * Access tokens expire after 15 min. This client:
 * 1. Parses JWT exp claim and proactively refreshes BEFORE expiry
 * 2. On 401 response, retries once with a fresh token
 * 3. If refresh fails (refresh token expired/revoked), calls onAuthFailed
 */
class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String?,
    private val onTokensRefreshed: (accessToken: String, refreshToken: String) -> Unit,
    private val onAuthFailed: () -> Unit
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    // ─── Public helpers ───────────────────────────────────────

    suspend fun googleVerify(req: GoogleVerifyRequest): Result<AuthResponse> =
        post("/auth/google/verify", req)

    suspend fun refresh(req: RefreshRequest): Result<RefreshResponse> =
        post("/auth/refresh", req)

    suspend fun getMe(): Result<MeResponse> = get("/auth/me")

    suspend fun logout(): Result<OkResponse> = post("/auth/logout", EmptyBody, requireAuth = true)

    suspend fun validateKey(req: KeyValidateRequest): Result<KeyValidateResponse> =
        post("/keys/validate", req, requireAuth = true)

    suspend fun linkKey(req: KeyValidateRequest): Result<KeyLinkResponse> =
        post("/keys/link", req, requireAuth = true)

    suspend fun listKeys(): Result<List<ApiKeyDto>> = get("/keys")

    suspend fun unlinkKey(req: UnlinkRequest): Result<OkResponse> =
        post("/keys/unlink", req, requireAuth = true)

    suspend fun getBalance(): Result<BalanceResponse> = get("/user/balance")

    suspend fun getTransactions(limit: Int = 20, offset: Int = 0): Result<TransactionsResponse> =
        get("/user/transactions?limit=$limit&offset=$offset")

    // ─── AI Chat (different base: ch.ate.my.id, auth via API key not JWT) ───

    /**
     * Send a non-streaming chat completion request to the AI backend.
     * Uses a different base URL (ch.ate.my.id/v1) and authenticates with the
     * user's API key (ch_...) instead of the mobile JWT.
     */
    suspend fun chat(apiKey: String, req: ChatRequest): Result<ChatResponse> {
        return runCatching {
            val chatUrl = "https://ch.ate.my.id/v1/chat/completions"
            val payload = json.encodeToString(serializer(), req)
            val request = Request.Builder()
                .url(chatUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            exec(request)
        }
    }

    /**
     * Fetch available AI models.
     */
    suspend fun chatModels(apiKey: String): Result<ChatModelsResponse> {
        return runCatching {
            val request = Request.Builder()
                .url("https://ch.ate.my.id/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            exec(request)
        }
    }

    /**
     * Send a vision chat request (with image) to the AI backend.
     * Used for food calorie scanning. Uses kimi-k2.5 (vision-capable model).
     * Returns the raw ChatResponse — caller must parse content JSON.
     */
    suspend fun chatVision(apiKey: String, req: VisionChatRequest): Result<ChatResponse> {
        return runCatching {
            val chatUrl = "https://ch.ate.my.id/v1/chat/completions"
            val payload = json.encodeToString(VisionChatRequest.serializer(), req)
            val request = Request.Builder()
                .url(chatUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia))
                .build()
            exec(request)
        }
    }

    // ─── Core request ─────────────────────────────────────────

    private inline fun <reified Req, reified Res> post(
        path: String,
        body: Req,
        requireAuth: Boolean = false
    ): Result<Res> {
        return runCatching {
            val payload = json.encodeToString(serializer(), body)
            val url = baseUrl.trimEnd('/') + path
            if (requireAuth) {
                execWithAuth(url, "POST", payload)
            } else {
                exec(buildRequest(url, "POST", payload, null))
            }
        }
    }

    // overload for empty body (logout)
    private inline fun <reified Res> post(
        path: String,
        @Suppress("UNUSED_PARAMETER") body: EmptyBody,
        requireAuth: Boolean = false
    ): Result<Res> {
        return runCatching {
            val url = baseUrl.trimEnd('/') + path
            val payload = "{}"
            if (requireAuth) {
                execWithAuth(url, "POST", payload)
            } else {
                exec(buildRequest(url, "POST", payload, null))
            }
        }
    }

    private inline fun <reified Res> get(path: String): Result<Res> {
        return runCatching {
            val url = baseUrl.trimEnd('/') + path
            execWithAuth(url, "GET", null)
        }
    }

    // ─── Auth-aware request with auto-refresh ────────────────

    /**
     * Executes a request with auto-refresh logic:
     * 1. Get access token (proactively refresh if expired)
     * 2. Send request
     * 3. On 401: try refresh once, then retry
     * 4. On success: parse and return
     *
     * IMPORTANT: onAuthFailed is ONLY called if the refresh token itself is
     * rejected (401 from /auth/refresh). Network errors or 5xx do NOT clear
     * the session — the user keeps their refresh token and can retry later.
     */
    private inline fun <reified Res> execWithAuth(
        url: String,
        method: String,
        body: String?
    ): Res {
        // First attempt: use cached token (or refresh if expired)
        var token = ensureAccessToken()
        var resp = client.newCall(buildRequest(url, method, body, token)).execute()
        var raw = resp.body?.string().orEmpty()

        // On 401, try refresh + retry once
        if (resp.code == 401) {
            token = tryRefresh(refreshTokenProvider() ?: "")
            if (token != null) {
                resp = client.newCall(buildRequest(url, method, body, token)).execute()
                raw = resp.body?.string().orEmpty()
            }
        }

        if (!resp.isSuccessful) {
            val msg = runCatching {
                json.decodeFromString<ErrorResponse>(raw).let { it.detail ?: it.message }
            }.getOrNull() ?: raw.ifBlank { "HTTP ${resp.code}" }
            // Only clear session if we got 401 AFTER attempting refresh
            // (meaning the refresh token itself was rejected)
            if (resp.code == 401 && token == null) onAuthFailed()
            throw ApiException(resp.code, msg)
        }
        return json.decodeFromString<Res>(raw)
    }

    private fun buildRequest(url: String, method: String, body: String?, token: String?): Request {
        val builder = Request.Builder().url(url)
        if (token != null) builder.header("Authorization", "Bearer $token")
        if (method == "POST") {
            builder.post((body ?: "{}").toRequestBody(jsonMedia))
        } else {
            builder.get()
        }
        return builder.build()
    }

    /**
     * Returns a valid (non-expired) access token, refreshing proactively if needed.
     * Returns null if no session exists.
     */
    private fun ensureAccessToken(): String? {
        val current = tokenProvider()
        // If no access token, try refresh
        if (current.isNullOrBlank()) {
            val rt = refreshTokenProvider() ?: return null
            return tryRefresh(rt)
        }
        // Check if token is expired (decode JWT exp claim)
        if (isTokenExpired(current)) {
            val rt = refreshTokenProvider() ?: return null
            return tryRefresh(rt) ?: current // fallback to expired if refresh fails
        }
        return current
    }

    /**
     * Decode JWT payload and check if exp is in the past.
     * Returns true if token is expired or cannot be parsed.
     */
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            // Base64url decode payload (part 1)
            val payload = parts[1]
            val decoded = java.util.Base64.getUrlDecoder().decode(payload)
            val jsonStr = String(decoded)
            // Extract "exp" field — simple string search to avoid JSON parsing lib
            val expIdx = jsonStr.indexOf("\"exp\"")
            if (expIdx < 0) return false // no exp claim, assume valid
            val afterExp = jsonStr.substring(expIdx + 6)
            // Find the number after "exp":
            val expStr = afterExp.takeWhile { it.isDigit() || it == ':' || it == ' ' }.filter { it.isDigit() }
            if (expStr.isBlank()) return false
            val exp = expStr.toLong()
            val now = System.currentTimeMillis() / 1000
            // Add 5 second buffer
            exp <= now + 5
        } catch (e: Exception) {
            false // if we can't parse, don't block the request — let server decide
        }
    }

    private val refreshLock = Any()

    @Volatile
    private var lastRefreshResult: String? = null
    @Volatile
    private var lastRefreshTime: Long = 0

    private fun tryRefresh(refreshToken: String): String? {
        if (refreshToken.isBlank()) return null

        // Synchronized block prevents concurrent refresh calls from racing
        // (backend uses token rotation — two concurrent refreshes would
        // invalidate each other).
        synchronized(refreshLock) {
            // If another thread just refreshed within the last 10 seconds,
            // reuse its result (the new access token is already saved).
            val now = System.currentTimeMillis()
            if (now - lastRefreshTime < 10_000 && lastRefreshResult != null) {
                // Return the token provider's current value (already updated)
                return tokenProvider()
            }

            return try {
                val req = Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/auth/refresh")
                    .post(
                        json.encodeToString(serializer(), RefreshRequest(refreshToken))
                            .toRequestBody(jsonMedia)
                    )
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    // Don't call onAuthFailed here — let execWithAuth decide.
                    // A 401 means refresh token is truly invalid; a 5xx or
                    // network error is transient and should NOT clear session.
                    lastRefreshResult = null
                    null
                } else {
                    val body = resp.body?.string().orEmpty()
                    val parsed = json.decodeFromString<RefreshResponse>(body)
                    onTokensRefreshed(parsed.access_token, parsed.refresh_token)
                    lastRefreshResult = parsed.access_token
                    lastRefreshTime = System.currentTimeMillis()
                    parsed.access_token
                }
            } catch (e: Exception) {
                // Network error, timeout, etc. — don't clear session!
                lastRefreshResult = null
                null
            }
        }
    }

    private inline fun <reified Res> exec(req: Request): Res {
        val resp = client.newCall(req).execute()
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            val msg = runCatching {
                json.decodeFromString<ErrorResponse>(raw).let { it.detail ?: it.message }
            }.getOrNull() ?: raw.ifBlank { "HTTP ${resp.code}" }
            throw ApiException(resp.code, msg)
        }
        return json.decodeFromString<Res>(raw)
    }
}

object EmptyBody

class ApiException(val code: Int, val errorBody: String) : Exception(errorBody)
