package com.phenom.gym.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenom.gym.R
import com.phenom.gym.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToGym: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        // === Greeting ===
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome back", color = TextSecondary, fontSize = 14.sp)
                    Text("Sylph GYM", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentPrimary, AccentCyan)))
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PH", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // === Hero Card (Sylph GYM banner) ===
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // Banner image background
                Image(
                    painter = painterResource(R.drawable.sylph_gym_banner),
                    contentDescription = "Sylph GYM",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.4f to Color.Transparent,
                                1f to BgDark.copy(alpha = 0.85f)
                            )
                        )
                )
                // Status badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    ModernBadge("Online", AccentGreen)
                    Spacer(Modifier.width(8.dp))
                    ModernBadge("Google Sync", Color.White)
                }
            }
        }

        // === Quick Access ===
        item {
            Text("Quick Access", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickCard("GYM", "Tracker latihan", AccentCyan, Icons.Filled.FitnessCenter, onNavigateToGym, Modifier.weight(1f))
                QuickCard("SCAN", "Deteksi kalori", AccentPink, Icons.Filled.PhotoCamera, onNavigateToScan, Modifier.weight(1f))
                QuickCard("PROFILE", "Akun & API key", AccentPrimary, Icons.Filled.Person, onNavigateToProfile, Modifier.weight(1f))
            }
        }

        // === Stats ===
        item {
            Text("This Week", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Streak", "7", "days", AccentOrange, Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                StatCard("Volume", "12.5k", "kg", AccentCyan, Icons.Filled.TrendingUp, Modifier.weight(1f))
                StatCard("Sessions", "4", "this week", AccentGreen, Icons.Filled.FitnessCenter, Modifier.weight(1f))
            }
        }

        // === Tip Card ===
        item {
            var tipIndex by remember { mutableIntStateOf((0 until 30).random()) }
            val tip = remember(tipIndex) { TipsProvider.getTip(tipIndex) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .clickable { tipIndex = (tipIndex + 1) % 30 }
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡 Tip of the Day", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Tap to change →",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"$tip\"",
                        color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, color: Color, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextTertiary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(unit, color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun ModernBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
