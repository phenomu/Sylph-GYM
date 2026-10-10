package com.phenom.gym.ui.chat

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.BuildConfig
import com.phenom.gym.data.local.chat.ChatDbHelper
import com.phenom.gym.data.local.chat.ChatSession
import com.phenom.gym.data.remote.ChatMessage
import com.phenom.gym.data.remote.FoodAnalysisResult
import com.phenom.gym.data.repo.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * UI model for a single chat bubble.
 */
data class ChatBubble(
    val role: String,        // "user" | "assistant" | "system"
    val content: String,
    val isError: Boolean = false,
    val tokens: Int? = null
)

/** AI personality / system prompt mode. */
enum class AiMode(val label: String, val systemPrompt: String) {
    GYM(
        "Gym",
        """Kamu adalah instruktur gym profesional bersertifikat dengan pengalaman 10+ tahun.
Tugas utama kamu: membantu user dengan workout plan, form correction, tips nutrisi, dan motivasi.

Aturan:
- Jawab dalam Bahasa Indonesia kecuali user minta English
- Selalu berikan jawaban praktis dan actionable
- Jika user bertanya tentang workout, berikan rencana spesifik (sets, reps, rest)
- Jika user bertanya tentang nutrisi, berikan contoh konkret
- Peringatkan user jika ada risiko cedera pada exercise tertentu
- Gunakan emoji yang relevan untuk membuat percakapan lebih hidup
- Jika user bertanya di luar topik gym/fitness/nutrisi, arahkan kembali ke topik fitness""".trimIndent()
    ),
    CODE(
        "Code",
        """Kamu adalah senior software engineer dengan keahlian di berbagai bahasa pemrograman.
Tugas utama kamu: membantu user dengan coding, debugging, architecture, dan best practices.

Aturan:
- Jawab dalam Bahasa Indonesia kecuali user minta English
- Selalu berikan code block dengan syntax yang benar
- Jelaskan reasoning di balik setiap solusi, bukan hanya code
- Sebutkan time/space complexity jika relevan
- Jika user berikan code yang error, jelaskan apa penyebabnya dan cara fix
- Rekomendasikan best practices (clean code, SOLID, design patterns)
- Jika ada multiple approach, jelaskan trade-offs masing-masing""".trimIndent()
    )
}

data class ChatState(
    val messages: List<ChatBubble> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasApiKey: Boolean = false,
    val apiKeyName: String? = null,
    val apiKeyPrefix: String? = null,
    val balance: Long? = null,
    val selectedModel: String = "glm-5.2",
    val selectedMode: AiMode = AiMode.GYM,
    // Multi-session support
    val currentSessionId: Long? = null,
    val sessions: List<ChatSession> = emptyList(),
    val showSessionsList: Boolean = false,
    // Scan mode state
    val scanResult: FoodAnalysisResult? = null,
    val scanImageUri: String? = null,
    val isScanning: Boolean = false,
    val scanError: String? = null
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(
        app.applicationContext,
        BuildConfig.API_BASE_URL,
        BuildConfig.GOOGLE_CLIENT_ID
    )

    private val chatDb = ChatDbHelper(app.applicationContext)

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        refreshStatus()
        loadSessions()
    }

    fun refreshStatus() {
        val key = repo.getApiKey()
        val (name, prefix) = repo.getApiKeyMeta()
        _state.update {
            it.copy(
                hasApiKey = !key.isNullOrBlank(),
                apiKeyName = name,
                apiKeyPrefix = prefix
            )
        }
        if (!key.isNullOrBlank()) {
            fetchBalance()
        }
    }

    fun setModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun setMode(mode: AiMode) {
        _state.update { it.copy(selectedMode = mode) }
    }

    private fun fetchBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getBalance().onSuccess { resp ->
                _state.update { it.copy(balance = resp.balance) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (!_state.value.hasApiKey) {
            _state.update { it.copy(error = "No API key linked. Add an API key in Profile → API Keys first.") }
            return
        }

        val userMsg = ChatBubble(role = "user", content = text)
        _state.update {
            it.copy(
                messages = it.messages + userMsg,
                isLoading = true,
                error = null
            )
        }

        // Ensure a session exists and save the user message
        val sessionId = ensureSession()
        saveMessageToSession(sessionId, userMsg)

        // Update session title to first user message (truncated)
        if (_state.value.messages.size == 1) {
            val titleText = text.take(40).ifBlank { "New Session" }
            chatDb.updateSessionTitle(sessionId, titleText)
            loadSessions()
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Build conversation: system prompt + history (excluding system bubbles)
            val mode = _state.value.selectedMode
            val model = _state.value.selectedModel

            val apiMessages = mutableListOf<ChatMessage>()
            // System prompt as first message — sets AI personality
            apiMessages.add(ChatMessage(role = "system", content = mode.systemPrompt))
            // Add conversation history (skip any system bubbles)
            _state.value.messages
                .filter { it.role != "system" }
                .forEach { msg ->
                    apiMessages.add(ChatMessage(role = msg.role, content = msg.content))
                }

            repo.chat(apiMessages, model = model).onSuccess { resp ->
                val assistantContent = resp.choices.firstOrNull()?.message?.content
                    ?: "(Empty response)"
                val tokens = resp.usage?.total_tokens
                val assistantBubble = ChatBubble(
                    role = "assistant",
                    content = assistantContent,
                    tokens = tokens
                )
                _state.update {
                    it.copy(
                        messages = it.messages + assistantBubble,
                        isLoading = false
                    )
                }
                // Save assistant reply to session
                saveMessageToSession(sessionId, assistantBubble)
                fetchBalance()  // update balance after usage
            }.onFailure { e ->
                val errorBubble = ChatBubble(
                    role = "assistant",
                    content = "Error: ${e.message ?: "Unknown error"}",
                    isError = true
                )
                _state.update {
                    it.copy(
                        messages = it.messages + errorBubble,
                        isLoading = false
                    )
                }
                saveMessageToSession(sessionId, errorBubble)
            }
        }
    }

    fun clearChat() {
        // If we have a current session, delete all messages in it
        _state.value.currentSessionId?.let { sid ->
            chatDb.deleteAllMessagesForSession(sid)
        }
        _state.update { it.copy(messages = emptyList(), error = null) }
    }

    // ─── Multi-session management ───

    private fun loadSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = chatDb.queryAllSessions()
            _state.update { it.copy(sessions = sessions) }
        }
    }

    fun toggleSessionsList() {
        _state.update { it.copy(showSessionsList = !it.showSessionsList) }
        loadSessions()
    }

    fun startNewSession() {
        _state.update {
            it.copy(
                messages = emptyList(),
                currentSessionId = null,
                showSessionsList = false,
                error = null
            )
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = chatDb.queryMessagesForSession(sessionId)
            val sessions = chatDb.queryAllSessions()
            val session = sessions.find { it.id == sessionId }
            _state.update {
                it.copy(
                    currentSessionId = sessionId,
                    messages = messages.map { m ->
                        ChatBubble(role = m.role, content = m.content, isError = m.isError, tokens = m.tokens)
                    },
                    sessions = sessions,
                    showSessionsList = false,
                    selectedMode = AiMode.entries.find { it.name == session?.mode } ?: AiMode.GYM,
                    selectedModel = session?.model ?: "glm-5.2"
                )
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDb.deleteSession(sessionId)
            if (_state.value.currentSessionId == sessionId) {
                _state.update { it.copy(currentSessionId = null, messages = emptyList()) }
            }
            loadSessions()
        }
    }

    private fun ensureSession(): Long {
        _state.value.currentSessionId?.let { return it }
        // Create a new session with the first user message as title (placeholder)
        val mode = _state.value.selectedMode
        val model = _state.value.selectedModel
        val title = "Sesi ${System.currentTimeMillis()}"
        val id = chatDb.insertSession(ChatSession(
            title = title,
            mode = mode.name,
            model = model
        ))
        _state.update { it.copy(currentSessionId = id) }
        return id
    }

    private fun saveMessageToSession(sessionId: Long, bubble: ChatBubble) {
        chatDb.insertMessage(
            com.phenom.gym.data.local.chat.ChatMessageRecord(
                sessionId = sessionId,
                role = bubble.role,
                content = bubble.content,
                isError = bubble.isError,
                tokens = bubble.tokens
            )
        )
        loadSessions()
    }

    // ─── Food Scan (AI Vision) ─────────────────────────────

    private val scanJson = Json { ignoreUnknownKeys = true; isLenient = true }

    fun scanFoodImage(imageUri: String) {
        _state.update {
            it.copy(
                scanImageUri = imageUri,
                isScanning = true,
                scanError = null,
                scanResult = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load & compress image to base64
                val app = getApplication<Application>()
                val resolver = app.contentResolver
                val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(android.net.Uri.parse(imageUri)))
                    ?: throw Exception("Cannot load image")

                // Compress to JPEG, max 1024px width
                val scaled = scaleBitmap(bitmap, 1024)
                val stream = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                // Call AI vision API
                repo.scanFood(base64).onSuccess { resp ->
                    val content = resp.choices.firstOrNull()?.message?.content ?: ""
                    // Parse JSON from response (strip markdown code blocks if present)
                    val cleanContent = stripMarkdownCodeBlocks(content)
                    val result = scanJson.decodeFromString<FoodAnalysisResult>(cleanContent)
                    _state.update {
                        it.copy(
                            scanResult = result,
                            isScanning = false
                        )
                    }
                    fetchBalance()
                }.onFailure { e ->
                    _state.update {
                        it.copy(
                            isScanning = false,
                            scanError = e.message ?: "Failed to scan food"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        scanError = e.message ?: "Failed to process image"
                    )
                }
            }
        }
    }

    fun clearScanResult() {
        _state.update {
            it.copy(scanResult = null, scanImageUri = null, scanError = null, isScanning = false)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    private fun stripMarkdownCodeBlocks(text: String): String {
        var clean = text.trim()
        // Remove ```json ... ``` or ``` ... ```
        if (clean.startsWith("```")) {
            // Remove first line (```json or ```)
            clean = clean.substringAfter("\n", clean)
            // Remove closing ```
            val lastFence = clean.lastIndexOf("```")
            if (lastFence >= 0) {
                clean = clean.substring(0, lastFence).trim()
            }
        }
        return clean.trim()
    }
}
