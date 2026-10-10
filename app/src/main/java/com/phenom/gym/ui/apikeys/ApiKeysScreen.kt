package com.phenom.gym.ui.apikeys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenom.gym.data.remote.ApiKeyDto
import com.phenom.gym.data.remote.KeyValidateResponse
import com.phenom.gym.ui.theme.*

@Composable
fun ApiKeysScreen(vm: ApiKeysViewModel, onBack: () -> Unit) {
    val keys by vm.keys.collectAsState()
    val input by vm.input.collectAsState()
    val validateRes by vm.validateResult.collectAsState()
    val busy by vm.busy.collectAsState()
    val msg by vm.message.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BgDark).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).clickable { onBack() }.padding(2.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("API Keys", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "Dapatkan API key dari bot Telegram @HephaPanel_bot (kirim /apikey), lalu paste di bawah.",
            color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp
        )

        // Input
        OutlinedTextField(
            value = input,
            onValueChange = vm::setInput,
            label = { Text("API Key (ch_...)", color = TextTertiary, fontSize = 13.sp) },
            singleLine = false,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = BgInput,
                unfocusedContainerColor = BgInput,
                cursorColor = AccentPrimary,
                focusedLabelColor = AccentPrimary,
                unfocusedLabelColor = TextTertiary
            )
        )

        // Validate result
        validateRes?.let { ValidateResultCard(it) }

        // Buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModernBtn("Validate", AccentCyan, busy, Modifier.weight(1f)) { vm.validate() }
            val canLink = validateRes?.can_link == true
            ModernBtn("Link", if (canLink) AccentGreen else TextTertiary, busy || !canLink, Modifier.weight(1f)) { vm.link() }
        }

        // Message
        msg?.let { m ->
            Text(
                m,
                color = if (m.contains("✓") || m.contains("berhasil", ignoreCase = true)) AccentGreen else AccentOrange,
                fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Linked keys
        Text("Linked Keys", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        if (keys.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada key terhubung", color = TextTertiary, fontSize = 13.sp)
            }
        } else {
            keys.forEach { KeyRow(it) { vm.unlink(it.id) } }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ValidateResultCard(res: KeyValidateResponse) {
    val color = when {
        res.valid && res.can_link == true -> AccentGreen
        res.valid && res.already_linked == true -> AccentYellow
        else -> AccentOrange
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Status: ${res.status ?: "—"}", color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("valid: ${res.valid}", color = TextSecondary, fontSize = 12.sp)
        Text("can_link: ${res.can_link}", color = TextSecondary, fontSize = 12.sp)
        res.owner_email?.let { Text("owner: $it", color = TextTertiary, fontSize = 12.sp) }
        res.message?.let { Text(it, color = TextTertiary, fontSize = 12.sp) }
    }
}

@Composable
private fun RowScope.ModernBtn(label: String, color: Color, disabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (disabled) 0.08f else 0.15f))
            .border(1.dp, color.copy(alpha = if (disabled) 0.15f else 0.4f), RoundedCornerShape(14.dp))
            .clickable(enabled = !disabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (disabled && label.contains("link", ignoreCase = true)) {
            Text(label, color = color.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KeyRow(key: ApiKeyDto, onUnlink: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(key.name ?: "key", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(key.prefix ?: "—", color = AccentPrimary, fontSize = 13.sp)
            Text("status: ${key.status}", color = TextTertiary, fontSize = 11.sp)
        }
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(AccentOrange.copy(alpha = 0.1f)).clickable { onUnlink() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Unlink", tint = AccentOrange, modifier = Modifier.size(16.dp))
        }
    }
}
