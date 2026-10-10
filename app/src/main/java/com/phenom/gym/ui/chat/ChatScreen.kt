package com.phenom.gym.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phenom.gym.data.local.chat.ChatSession
import com.phenom.gym.ui.theme.*

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    // Two modes: AI Chat vs Scan
    var mode by remember { mutableStateOf(ChatMode.AI) }

    // Refresh API-key status whenever the screen resumes (e.g. after unlinking
    // a key in Profile → API Keys and coming back to Chat).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding()
            // Leave space for bottom nav bar (~72dp)
            .padding(bottom = 72.dp)
    ) {
        // ─── Top Bar ───
        TopBar()

        // ─── Mode Switcher ───
        ModeSwitcher(selected = mode, onSelect = { mode = it })

        // ─── Content ───
        when (mode) {
            ChatMode.AI -> {
                // API key status banner
                if (!state.hasApiKey) {
                    NoApiKeyBanner()
                }

                // Messages list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.messages.isEmpty() && state.hasApiKey) {
                        EmptyChatState()
                    } else if (state.messages.isEmpty() && !state.hasApiKey) {
                        Spacer(Modifier)
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.messages) { bubble ->
                                ChatBubbleView(bubble)
                            }
                            if (state.isLoading) {
                                item { TypingIndicator() }
                            }
                        }
                    }
                }

                // Balance + clear button row
                if (state.hasApiKey) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (state.balance != null) "Saldo: ${state.balance}" else "Saldo: ...",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                        if (state.messages.isNotEmpty()) {
                            IconButton(onClick = { vm.clearChat() }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Clear chat",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Model + Mode selector row
                if (state.hasApiKey) {
                    ModelModeRow(
                        selectedModel = state.selectedModel,
                        selectedMode = state.selectedMode,
                        onModelChange = { vm.setModel(it) },
                        onModeChange = { vm.setMode(it) }
                    )
                }

                // Input bar
                if (state.hasApiKey) {
                    ChatInputBar(
                        text = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank()) {
                                vm.sendMessage(inputText.trim())
                                inputText = ""
                                keyboard?.hide()
                            }
                        },
                        enabled = !state.isLoading
                    )
                }

                // Sessions list card
                if (state.hasApiKey) {
                    SessionsListCard(
                        sessions = state.sessions,
                        currentSessionId = state.currentSessionId,
                        onToggle = { vm.toggleSessionsList() },
                        onSelect = { vm.loadSession(it) },
                        onNew = { vm.startNewSession() },
                        onDelete = { vm.deleteSession(it) },
                        expanded = state.showSessionsList
                    )
                }
            }
            ChatMode.SCAN -> {
                ScanScreen(vm = vm)
            }
        }
    }
}

// ─── Components ──────────────────────────────────────────────

enum class ChatMode(val label: String) {
    AI("AI Chat"),
    SCAN("Scan Kalori")
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(AccentPrimary, AccentCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text("Sylph AI", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModeSwitcher(selected: ChatMode, onSelect: (ChatMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(4.dp)
    ) {
        ChatMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) Modifier.background(
                            Brush.horizontalGradient(listOf(AccentPrimary, AccentCyan.copy(alpha = 0.8f)))
                        )
                        else Modifier
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.label,
                    color = if (isSelected) Color.White else TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun NoApiKeyBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentYellow.copy(alpha = 0.1f))
            .padding(20.dp)
    ) {
        Text("API Key Belum Terhubung", color = AccentYellow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Untuk menggunakan AI Chat, hubungkan API key dari @HepnaPanel_bot di Profile → API Keys.",
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.radialGradient(listOf(AccentPrimary, Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Mulai percakapan", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tanya apa saja tentang fitness, nutrisi, atau workout plan",
            color = TextTertiary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ChatBubbleView(bubble: ChatBubble) {
    val isUser = bubble.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Assistant avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(AccentPrimary, AccentCyan))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isUser) AccentPrimary.copy(alpha = 0.15f)
                    else if (bubble.isError) AccentPink.copy(alpha = 0.1f)
                    else BgCard
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                bubble.content,
                color = if (bubble.isError) AccentPink else TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            if (bubble.tokens != null) {
                Spacer(Modifier.height(4.dp))
                Text("${bubble.tokens} tokens", color = TextTertiary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(AccentPrimary, AccentCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SmartToy, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(BgCard)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text("mengetik...", color = TextTertiary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ModelModeRow(
    selectedModel: String,
    selectedMode: AiMode,
    onModelChange: (String) -> Unit,
    onModeChange: (AiMode) -> Unit
) {
    var modelExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    val models = listOf("glm-5.2", "combo1", "coder", "hepha-1", "hepha-2", "murah")
    val modes = AiMode.entries

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Model dropdown
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                label = "Model",
                value = selectedModel,
                expanded = modelExpanded,
                onClick = { modelExpanded = !modelExpanded; modeExpanded = false }
            )
            DropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false },
                modifier = Modifier.background(BgCardAlt)
            ) {
                models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m, color = if (m == selectedModel) AccentCyan else TextPrimary) },
                        onClick = { onModelChange(m); modelExpanded = false }
                    )
                }
            }
        }

        // Mode dropdown
        Box(modifier = Modifier.weight(1f)) {
            DropdownChip(
                label = "Mode",
                value = selectedMode.label,
                expanded = modeExpanded,
                onClick = { modeExpanded = !modeExpanded; modelExpanded = false }
            )
            DropdownMenu(
                expanded = modeExpanded,
                onDismissRequest = { modeExpanded = false },
                modifier = Modifier.background(BgCardAlt)
            ) {
                modes.forEach { md ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(md.label, color = if (md == selectedMode) AccentCyan else TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (md == AiMode.GYM) "Instruktur gym profesional" else "Senior software engineer",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        onClick = { onModeChange(md); modeExpanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownChip(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) AccentPrimary.copy(alpha = 0.15f) else BgCard)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextTertiary, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        Icon(
            if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Tulis pesan...", color = TextTertiary, fontSize = 14.sp) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = BorderSubtle,
                cursorColor = AccentPrimary,
                focusedContainerColor = BgInput,
                unfocusedContainerColor = BgInput
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            trailingIcon = {
                IconButton(onClick = onSend, enabled = enabled && text.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) AccentCyan else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            enabled = enabled
        )
    }
}

@Composable
private fun ScanPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.radialGradient(listOf(AccentCyan.copy(alpha = 0.2f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CameraAlt, null, tint = AccentCyan, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Scan Kalori Makanan", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Coming Soon", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Foto makanan → AI deteksi kalori, protein, karbohidrat, dan lemak otomatis.",
            color = TextTertiary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text("Butuh API key terhubung untuk akses AI vision.", color = TextTertiary, fontSize = 12.sp)
    }
}

// ─── Sessions List Card ──────────────────────────────────────

@Composable
private fun SessionsListCard(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onToggle: () -> Unit,
    onSelect: (Long) -> Unit,
    onNew: () -> Unit,
    onDelete: (Long) -> Unit,
    expanded: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        // Header row — tap to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ChatBubble,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sessions",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "(${sessions.size})",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Expanded list
        if (expanded) {
            // New session button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNew)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Session", color = AccentPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            if (sessions.isEmpty()) {
                Text(
                    "Belum ada sesi tersimpan. Mulai chat untuk membuat sesi baru.",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                sessions.forEach { session ->
                    SessionRow(
                        session = session,
                        isActive = session.id == currentSessionId,
                        onClick = { onSelect(session.id) },
                        onDelete = { onDelete(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(session.createdAt) {
        val date = java.util.Date(session.createdAt)
        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        sdf.format(date)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isActive) AccentPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.title,
                color = if (isActive) AccentCyan else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
            Text(
                "$dateStr • ${session.mode}",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete session",
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
