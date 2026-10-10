package com.phenom.gym.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.phenom.gym.ui.theme.*

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onLogout: () -> Unit,
    onApiKeys: () -> Unit
) {
    // Refresh on resume so saldo / API-key status updates after unlinking
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val user by vm.user.collectAsState()
    val balance by vm.balance.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    Column(
        Modifier.fillMaxSize().background(BgDark).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Profile", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentOrange.copy(alpha = 0.1f))
                    .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { vm.logout(onLogout) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = AccentOrange, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Logout", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Avatar (rounded, not hex)
        Box(Modifier.size(100.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentPrimary, AccentCyan))), contentAlignment = Alignment.Center) {
            if (user?.picture != null) {
                AsyncImage(model = user!!.picture, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Text((user?.name ?: user?.email ?: "PH").take(2).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Name + email
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text((user?.name ?: "Sylph GYM"), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(user?.email ?: user?.username ?: "—", color = TextSecondary, fontSize = 13.sp)
        }

        // Status badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge("Online", AccentGreen)
            StatusBadge("Verified", AccentCyan)
            if (balance != null) StatusBadge("Linked", AccentYellow)
        }

        // Balance or link prompt
        if (balance != null) {
            BalanceCard(balance = balance!!.balance, multiplier = balance!!.charge_multiplier)
        } else {
            LinkKeyCard(onApiKeys = onApiKeys)
        }

        // Error
        error?.let { e ->
            Text(e, color = AccentOrange, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }

        // Stats row
        StatRow(balance = balance?.balance, multiplier = balance?.charge_multiplier)

        // API Keys shortcut
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .clickable(onClick = onApiKeys)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("API Keys", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Kelola API key dari @HephaPanel_bot", color = TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
        }

        // Refresh button
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AccentPrimary.copy(alpha = 0.1f))
                .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable(enabled = !loading) { vm.refresh() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(color = AccentPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Syncing...", color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Refresh Data", color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BalanceCard(balance: Long, multiplier: Int) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(AccentGreen.copy(alpha = 0.15f), BgCard)))
            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Saldo", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(formatBalance(balance), color = AccentGreen, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("charge ×$multiplier", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun LinkKeyCard(onApiKeys: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, AccentYellow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onApiKeys)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No API Key Linked", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Hubungkan API key dari @HephaPanel_bot untuk melihat saldo", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Tap to link →", color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatRow(balance: Long?, multiplier: Int?) {
    val stats = listOf(
        Triple("Saldo", balance?.let { formatBalance(it) } ?: "—", AccentGreen),
        Triple("Multiplier", multiplier?.let { "×$it" } ?: "—", AccentCyan),
        Triple("Status", if (balance != null) "OK" else "—", AccentYellow)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { (label, value, color) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)).padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(label, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

private fun formatBalance(v: Long): String {
    val s = v.toString()
    return buildString {
        var c = 0
        for (i in s.indices.reversed()) { insert(0, s[i]); c++; if (c % 3 == 0 && i != 0) insert(0, '.') }
    }
}
