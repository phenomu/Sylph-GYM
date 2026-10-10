package com.phenom.gym.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phenom.gym.data.remote.FoodAnalysisResult
import com.phenom.gym.data.remote.FoodItem
import com.phenom.gym.ui.theme.*

@Composable
fun ScanScreen(
    vm: ChatViewModel
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.scanFoodImage(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ─── Header ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CameraAlt, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Scan Kalori", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        when {
            // ─── Loading state ───
            state.isScanning -> {
                ScanningState(imageUri = state.scanImageUri)
            }

            // ─── Error state ───
            state.scanError != null -> {
                ErrorState(
                    error = state.scanError!!,
                    onRetry = { state.scanImageUri?.let { vm.scanFoodImage(it) } },
                    onNewScan = { vm.clearScanResult() }
                )
            }

            // ─── Result state ───
            state.scanResult != null -> {
                ScanResultView(
                    result = state.scanResult!!,
                    imageUri = state.scanImageUri,
                    onNewScan = { vm.clearScanResult() }
                )
            }

            // ─── Initial state (no image selected) ───
            else -> {
                InitialState(
                    hasApiKey = state.hasApiKey,
                    onPickImage = { imagePicker.launch("image/*") }
                )
            }
        }
    }
}

// ─── Components ──────────────────────────────────────────────

@Composable
private fun InitialState(hasApiKey: Boolean, onPickImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasApiKey) {
            // No API key warning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentYellow.copy(alpha = 0.1f))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("API Key Belum Terhubung", color = AccentYellow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hubungkan API key di Profile → API Keys untuk menggunakan scan kalori.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Upload prompt
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.radialGradient(listOf(AccentCyan.copy(alpha = 0.2f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AddAPhoto, null, tint = AccentCyan, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Scan Kalori Makanan", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload foto makanan → AI deteksi kalori, protein, karbohidrat, dan lemak otomatis",
                color = TextTertiary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onPickImage,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pilih Gambar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ScanningState(imageUri: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(24.dp))
        }
        CircularProgressIndicator(
            color = AccentCyan,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Menganalisis gambar...", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("AI sedang mengidentifikasi makanan & nutrisi", color = TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, onNewScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Error, null, tint = AccentPink, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Gagal Menganalisis", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(error, color = TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Coba Lagi")
            }
            Button(onClick = onNewScan, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)) {
                Text("Gambar Baru")
            }
        }
    }
}

@Composable
private fun ScanResultView(
    result: FoodAnalysisResult,
    imageUri: String?,
    onNewScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ─── Food image + name ───
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
        }

        // ─── Food name ───
        Text(result.foodName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            ConfidenceBadge(result.confidence)
            if (result.items.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text("${result.items.size} items", color = TextTertiary, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─── Total nutrition summary cards (2-3 per row) ───
        Text("Total Nutrisi", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        // Show only non-zero totals in a grid (2 per row)
        val totals = buildTotalList(result)
        NutritionGrid(items = totals)

        Spacer(Modifier.height(20.dp))

        // ─── Per-item breakdown ───
        if (result.items.isNotEmpty()) {
            Text("Detail per Item", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            result.items.forEach { item ->
                FoodItemCard(item)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ─── Notes ───
        if (!result.notes.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .padding(12.dp)
            ) {
                Row {
                    Icon(Icons.Filled.Info, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(result.notes, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── New scan button ───
        Button(
            onClick = onNewScan,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
        ) {
            Icon(Icons.Filled.AddAPhoto, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan Gambar Baru", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Build list of non-zero total nutrition values for grid display. */
private fun buildTotalList(result: FoodAnalysisResult): List<NutritionCardData> {
    val list = mutableListOf<NutritionCardData>()
    if (result.totalCalories > 0) list.add(NutritionCardData("Kalori", result.totalCalories.toInt().toString(), "kcal", AccentOrange))
    if (result.totalProteinG > 0) list.add(NutritionCardData("Protein", formatNum(result.totalProteinG), "g", AccentCyan))
    if (result.totalCarbsG > 0) list.add(NutritionCardData("Karbo", formatNum(result.totalCarbsG), "g", AccentGreen))
    if (result.totalFatG > 0) list.add(NutritionCardData("Lemak", formatNum(result.totalFatG), "g", AccentYellow))
    if (result.totalFiberG > 0) list.add(NutritionCardData("Serat", formatNum(result.totalFiberG), "g", AccentPrimary))
    if (result.totalSugarG > 0) list.add(NutritionCardData("Gula", formatNum(result.totalSugarG), "g", AccentPink))
    if (result.totalSodiumMg > 0) list.add(NutritionCardData("Natrium", result.totalSodiumMg.toInt().toString(), "mg", TextSecondary))
    return list
}

@Composable
private fun NutritionGrid(items: List<NutritionCardData>) {
    // 2 items per row
    items.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEach { item ->
                NutritionCard(item, modifier = Modifier.weight(1f))
            }
            // Fill empty slot if odd number
            if (rowItems.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NutritionCard(data: NutritionCardData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(data.label, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(data.value, color = data.color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            Text(data.unit, color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun FoodItemCard(item: FoodItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.calories.toInt()} kcal", color = AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        // Micro nutrients — only show non-zero
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (item.proteinG > 0) {
                MicroNutrient("P", formatNum(item.proteinG), AccentCyan)
            }
            if (item.carbsG > 0) {
                MicroNutrient("C", formatNum(item.carbsG), AccentGreen)
            }
            if (item.fatG > 0) {
                MicroNutrient("F", formatNum(item.fatG), AccentYellow)
            }
            if (item.fiberG > 0) {
                MicroNutrient("Fi", formatNum(item.fiberG), AccentPrimary)
            }
            if (item.sugarG > 0) {
                MicroNutrient("S", formatNum(item.sugarG), AccentPink)
            }
        }
    }
}

@Composable
private fun MicroNutrient(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ", color = TextTertiary, fontSize = 11.sp)
        Text("${value}g", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConfidenceBadge(confidence: String) {
    val color = when (confidence.lowercase()) {
        "high" -> AccentGreen
        "medium" -> AccentYellow
        else -> AccentPink
    }
    val text = when (confidence.lowercase()) {
        "high" -> "Akurasi Tinggi"
        "medium" -> "Akurasi Sedang"
        else -> "Akurasi Rendah"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private data class NutritionCardData(
    val label: String,
    val value: String,
    val unit: String,
    val color: Color
)

private fun formatNum(d: Double): String {
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else String.format("%.1f", d)
}
