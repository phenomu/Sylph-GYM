package com.phenom.gym.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenom.gym.R
import com.phenom.gym.ui.login.LoginUi
import com.phenom.gym.ui.theme.*

@Composable
fun LoginScreen(vm: LoginViewModel, onLoggedIn: () -> Unit) {
    val state by vm.ui.collectAsState()
    val already by vm.alreadyLoggedIn.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(already) { if (already) onLoggedIn() }
    LaunchedEffect(state) {
        if (state is LoginUi.Success) { onLoggedIn(); vm.consumeSuccess() }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BgDark, Color(0xFF08090C), BgDark))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo — Sylph bird (transparent) di lingkaran besar
            Box(
                Modifier.size(110.dp).clip(CircleShape)
                    .background(BgDark),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_sylph_bird),
                    contentDescription = "Sylph GYM",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Sylph GYM", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("AI Fitness Terminal", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Masuk untuk menghubungkan API key dari @HephaPanel_bot",
                color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))

            // Google button
            val loading = state is LoginUi.Loading
            GoogleButton(loading = loading, onClick = { if (!loading) vm.signInWithGoogle(context) })

            Spacer(Modifier.height(16.dp))
            (state as? LoginUi.Error)?.let { err ->
                Text(
                    err.message,
                    color = AccentOrange,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentOrange.copy(alpha = 0.1f))
                        .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                )
            }

            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sesi disimpan terenkripsi di perangkat", color = TextTertiary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF4285F4), Color(0xFF1A73E8))))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Authenticating...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
