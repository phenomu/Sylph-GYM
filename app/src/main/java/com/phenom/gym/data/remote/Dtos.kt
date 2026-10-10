package com.phenom.gym.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Auth ──────────────────────────────────────────────────────

@Serializable
data class GoogleVerifyRequest(
    val id_token: String,
    val device_id: String,
    val device_name: String? = null,
    val platform: String = "android",
    val app_version: String? = null
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long = 900,
    val user: UserDto,
    val api_keys: List<ApiKeyDto> = emptyList()
)

@Serializable
data class RefreshRequest(
    val refresh_token: String
)

@Serializable
data class RefreshResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long = 900
)

@Serializable
data class MeResponse(
    val user: UserDto,
    val session: SessionDto? = null
)

@Serializable
data class LogoutResponse(
    val ok: Boolean = true
)

// ─── User ──────────────────────────────────────────────────────

@Serializable
data class UserDto(
    val telegram_id: Long? = null,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null,
    val username: String? = null,
    val balance: Long? = null,
    val charge_multiplier: Int? = null
)

@Serializable
data class SessionDto(
    val device_name: String? = null,
    val platform: String? = null,
    val last_active_at: String? = null,
    val expires_at: String? = null
)

// ─── API Keys ──────────────────────────────────────────────────

@Serializable
data class KeyValidateRequest(
    val api_key: String
)

@Serializable
data class KeyValidateResponse(
    val valid: Boolean,
    val status: String? = null,
    val already_linked: Boolean? = null,
    val owner_email: String? = null,
    val can_link: Boolean? = null,
    val message: String? = null
)

@Serializable
data class KeyLinkResponse(
    val ok: Boolean = true,
    val user: UserDto? = null,
    val api_key_info: ApiKeyInfoDto? = null
)

@Serializable
data class ApiKeyInfoDto(
    val id: Long,
    val name: String? = null,
    val prefix: String? = null
)

@Serializable
data class ApiKeyDto(
    val id: Long,
    val name: String? = null,
    val prefix: String? = null,
    val status: String? = null,
    val created_at: String? = null,
    val last_used_at: String? = null
)

@Serializable
data class UnlinkRequest(
    val api_key_id: Long
)

@Serializable
data class OkResponse(
    val ok: Boolean = true
)

// ─── Balance / Transactions ──────────────────────────────────

@Serializable
data class BalanceResponse(
    val balance: Long,
    val charge_multiplier: Int
)

@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionDto>,
    val limit: Int,
    val offset: Int
)

@Serializable
data class TransactionDto(
    val id: Long,
    val type: String,
    val amount: Long,
    val tokens_used: Double? = null,
    val model: String? = null,
    val note: String? = null,
    val created_at: String
)

// ─── Error ────────────────────────────────────────────────────

@Serializable
data class ErrorResponse(
    val detail: String? = null,
    val message: String? = null
)

// ─── AI Chat ──────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    val model: String = "glm-5.2",
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = 32000
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

// ─── AI Vision (food calorie scan) ───────────────────────────

/**
 * Content block for vision chat messages.
 * Can be text or image_url (base64 data URI).
 */
@Serializable
data class VisionContentPart(
    val type: String, // "text" | "image_url"
    val text: String? = null,
    val image_url: VisionImageUrl? = null
)

@Serializable
data class VisionImageUrl(
    val url: String // data:image/jpeg;base64,...
)

@Serializable
data class VisionChatMessage(
    val role: String,
    val content: List<VisionContentPart>
)

@Serializable
data class VisionChatRequest(
    val model: String = "kimi-k2.5",
    val messages: List<VisionChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = 32000
)

// ─── Food nutrition analysis result (parsed from AI JSON output) ───

@Serializable
data class FoodItem(
    val name: String = "",
    val calories: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("fiber_g") val fiberG: Double = 0.0,
    @SerialName("sugar_g") val sugarG: Double = 0.0,
    @SerialName("sodium_mg") val sodiumMg: Double = 0.0
)

@Serializable
data class FoodAnalysisResult(
    @SerialName("food_name") val foodName: String = "",
    val items: List<FoodItem> = emptyList(),
    @SerialName("total_calories") val totalCalories: Double = 0.0,
    @SerialName("total_protein_g") val totalProteinG: Double = 0.0,
    @SerialName("total_carbs_g") val totalCarbsG: Double = 0.0,
    @SerialName("total_fat_g") val totalFatG: Double = 0.0,
    @SerialName("total_fiber_g") val totalFiberG: Double = 0.0,
    @SerialName("total_sugar_g") val totalSugarG: Double = 0.0,
    @SerialName("total_sodium_mg") val totalSodiumMg: Double = 0.0,
    val confidence: String = "medium",
    val notes: String? = null
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val usage: ChatUsage? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

@Serializable
data class ChatModelsResponse(
    val `object`: String = "list",
    val data: List<ChatModel> = emptyList()
)

@Serializable
data class ChatModel(
    val id: String,
    val `object`: String = "model",
    val owned_by: String? = null
)
