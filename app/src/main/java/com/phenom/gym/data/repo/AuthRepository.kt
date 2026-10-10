package com.phenom.gym.data.repo

import android.content.Context
import android.os.Build
import com.phenom.gym.data.local.SessionManager
import com.phenom.gym.data.remote.ApiClient
import com.phenom.gym.data.remote.ApiException
import com.phenom.gym.data.remote.AuthResponse
import com.phenom.gym.data.remote.BalanceResponse
import com.phenom.gym.data.remote.ChatMessage
import com.phenom.gym.data.remote.ChatModelsResponse
import com.phenom.gym.data.remote.ChatRequest
import com.phenom.gym.data.remote.ChatResponse
import com.phenom.gym.data.remote.GoogleSignInHelper
import com.phenom.gym.data.remote.GoogleVerifyRequest
import com.phenom.gym.data.remote.KeyLinkResponse
import com.phenom.gym.data.remote.KeyValidateRequest
import com.phenom.gym.data.remote.KeyValidateResponse
import com.phenom.gym.data.remote.MeResponse
import com.phenom.gym.data.remote.OkResponse
import com.phenom.gym.data.remote.RefreshRequest
import com.phenom.gym.data.remote.RefreshResponse
import com.phenom.gym.data.remote.TransactionsResponse
import com.phenom.gym.data.remote.UnlinkRequest
import com.phenom.gym.data.remote.UserDto
import com.phenom.gym.data.remote.VisionChatMessage
import com.phenom.gym.data.remote.VisionChatRequest
import com.phenom.gym.data.remote.VisionContentPart
import com.phenom.gym.data.remote.VisionImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Repository exposing all 12 endpoints of the mobile API.
 * Holds a single ApiClient instance; tokens are injected via SessionManager.
 */
class AuthRepository(context: Context, private val baseUrl: String, private val googleClientId: String) {

    private val session = SessionManager(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val googleHelper = GoogleSignInHelper(context, googleClientId)

    val api: ApiClient = ApiClient(
        baseUrl = baseUrl,
        tokenProvider = { session.getAccessToken() },
        refreshTokenProvider = { session.getRefreshToken() },
        onTokensRefreshed = { at, rt -> session.saveTokens(at, rt) },
        onAuthFailed = { session.clear() }
    )

    fun isLoggedIn(): Boolean = session.isLoggedIn()
    fun getUser(): UserDto? = session.getUserJson()?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() }
    private fun cacheUser(u: UserDto) {
        session.saveUserJson(json.encodeToString(UserDto.serializer(), u))
    }

    // ─── Auth ───────────────────────────────────────────────

    suspend fun signInWithGoogle(activityContext: Context): Result<AuthResponse> = withContext(Dispatchers.IO) {
        val idToken = googleHelper.getIdToken(activityContext).getOrElse { return@withContext Result.failure(it) }
        val req = GoogleVerifyRequest(
            id_token = idToken,
            device_id = getDeviceId(),
            device_name = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            platform = "android",
            app_version = "1.0"
        )
        val res = api.googleVerify(req)
        res.onSuccess {
            session.saveTokens(it.access_token, it.refresh_token)
            cacheUser(it.user)
        }
    }

    suspend fun refresh(): Result<RefreshResponse> = withContext(Dispatchers.IO) {
        val rt = session.getRefreshToken() ?: return@withContext Result.failure(IllegalStateException("No refresh token"))
        api.refresh(RefreshRequest(rt)).onSuccess {
            session.saveTokens(it.access_token, it.refresh_token)
        }
    }

    suspend fun getMe(): Result<MeResponse> = withContext(Dispatchers.IO) {
        api.getMe().onSuccess { cacheUser(it.user) }
    }

    suspend fun logout(): Result<OkResponse> = withContext(Dispatchers.IO) {
        api.logout().also { session.clear() }
    }

    // ─── API Keys ──────────────────────────────────────────

    suspend fun validateKey(key: String): Result<KeyValidateResponse> = withContext(Dispatchers.IO) {
        api.validateKey(KeyValidateRequest(key))
    }

    suspend fun linkKey(key: String): Result<KeyLinkResponse> = withContext(Dispatchers.IO) {
        api.linkKey(KeyValidateRequest(key)).onSuccess {
            // Save full API key locally for AI chat use
            session.saveApiKey(key)
            it.api_key_info?.let { info ->
                session.saveApiKeyMeta(info.name, info.prefix)
            }
        }
    }

    suspend fun listKeys(): Result<List<ApiKeyDtoAlias>> = withContext(Dispatchers.IO) {
        api.listKeys()
    }

    suspend fun unlinkKey(id: Long): Result<OkResponse> = withContext(Dispatchers.IO) {
        api.unlinkKey(UnlinkRequest(id)).onSuccess {
            // Clear the locally-stored API key so AI Chat / Scan / balance
            // stop working until a new key is linked again.
            session.clearApiKey()
        }
    }

    // ─── User ──────────────────────────────────────────────

    suspend fun getBalance(): Result<BalanceResponse> = withContext(Dispatchers.IO) {
        api.getBalance()
    }

    suspend fun getTransactions(limit: Int = 20, offset: Int = 0): Result<TransactionsResponse> = withContext(Dispatchers.IO) {
        api.getTransactions(limit, offset)
    }

    // ─── AI Chat ──────────────────────────────────────────

    fun getApiKey(): String? = session.getApiKey()

    fun getApiKeyMeta(): Pair<String?, String?> = session.getApiKeyMeta()

    /** Save a validated API key locally for AI chat use (called when already_linked). */
    fun saveKeyForChat(key: String) {
        session.saveApiKey(key)
    }

    suspend fun chat(messages: List<ChatMessage>, model: String = "glm-5.2"): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            val key = session.getApiKey() ?: return@withContext Result.failure(
                IllegalStateException("No API key linked. Add an API key first.")
            )
            val req = ChatRequest(model = model, messages = messages, stream = false)
            api.chat(key, req)
        }

    suspend fun chatModels(): Result<ChatModelsResponse> = withContext(Dispatchers.IO) {
        val key = session.getApiKey() ?: return@withContext Result.failure(
            IllegalStateException("No API key linked")
        )
        api.chatModels(key)
    }

    /**
     * Scan a food image using AI vision (kimi-k2.5).
     * Returns the raw ChatResponse — caller must parse the JSON content.
     */
    suspend fun scanFood(imageBase64: String): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            val key = session.getApiKey() ?: return@withContext Result.failure(
                IllegalStateException("No API key linked. Add an API key first.")
            )
            val systemPrompt = """
                You are a nutrition expert AI. Analyze the food in the image and return ONLY a valid JSON object (no markdown, no code blocks, no explanation) with this structure:
                {"food_name":"brief name","items":[{"name":"item","calories":0,"protein_g":0,"carbs_g":0,"fat_g":0,"fiber_g":0,"sugar_g":0,"sodium_mg":0}],"total_calories":0,"total_protein_g":0,"total_carbs_g":0,"total_fat_g":0,"total_fiber_g":0,"total_sugar_g":0,"total_sodium_mg":0,"confidence":"high","notes":"brief notes"}
                Rules:
                - All numeric values must be numbers (int or float), never strings
                - If a nutrient value is 0 or not applicable, still include it as 0
                - Estimate portion sizes based on the image
                - Identify each visible food item separately
                - Return ONLY the JSON object
            """.trimIndent()

            val req = VisionChatRequest(
                model = "kimi-k2.5",
                messages = listOf(
                    VisionChatMessage(
                        role = "system",
                        content = listOf(VisionContentPart(type = "text", text = systemPrompt))
                    ),
                    VisionChatMessage(
                        role = "user",
                        content = listOf(
                            VisionContentPart(type = "text", text = "Analyze this meal and provide complete nutrition information as JSON."),
                            VisionContentPart(
                                type = "image_url",
                                image_url = VisionImageUrl(url = "data:image/jpeg;base64,$imageBase64")
                            )
                        )
                    )
                ),
                stream = false,
                temperature = 0.2
            )
            api.chatVision(key, req)
        }

    private fun getDeviceId(): String {
        // Stable but non-sensitive device identifier
        val base = (Build.MODEL + "_" + Build.BOARD).hashCode().toString(16)
        return "android-$base"
    }
}

// typealias to avoid extra import surface in UI layer
typealias ApiKeyDtoAlias = com.phenom.gym.data.remote.ApiKeyDto
