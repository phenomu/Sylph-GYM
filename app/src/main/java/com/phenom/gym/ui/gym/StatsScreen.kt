package com.phenom.gym.ui.gym

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phenom.gym.data.local.gym.ActivityDay
import com.phenom.gym.data.local.gym.BodyMetric
import com.phenom.gym.data.local.gym.CardioLog
import com.phenom.gym.data.repo.GymRepository
import com.phenom.gym.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(vm: StatsViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        // === HEADER ===
        item {
            Column {
                Text("Statistics", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Progress & analytics", color = TextSecondary, fontSize = 13.sp)
            }
        }

        // === OVERVIEW STATS ===
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OverviewCard("🔥 Streak", "${state.streak}", "days", Modifier.weight(1f))
                OverviewCard("💪 W. Cal", "${state.estimatedWorkoutCalories}", "kcal", Modifier.weight(1f))
                OverviewCard("🏃 C. Cal", "${state.totalCardioCalories}", "kcal", Modifier.weight(1f))
            }
        }

        // === BODY WEIGHT & BMI ===
        item {
            val showAddMetric by remember { mutableStateOf(false) }
            var showMetricDialog by remember { mutableStateOf(false) }

            StatsCard(
                title = "Body Weight & BMI",
                icon = Icons.Filled.MonitorWeight,
                action = { showMetricDialog = true }
            ) {
                val latest = state.bodyMetrics.lastOrNull()
                if (latest != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Weight", color = TextTertiary, fontSize = 11.sp)
                            Text("${latest.weightKg} kg", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Height", color = TextTertiary, fontSize = 11.sp)
                            Text("${latest.heightCm.toInt()} cm", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("BMI", color = TextTertiary, fontSize = 11.sp)
                            Text(state.bmi?.let { "%.1f".format(it) } ?: "—", color = AccentCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(state.bmiCategory, color = TextTertiary, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Weight chart line
                    if (state.bodyMetrics.size >= 2) {
                        WeightLineChart(state.bodyMetrics)
                    }
                } else {
                    Text("Belum ada data. Tap + untuk tambah berat badan.", color = TextTertiary, fontSize = 12.sp)
                }
            }

            if (showMetricDialog) {
                AddMetricDialog(
                    onDismiss = { showMetricDialog = false },
                    onSave = { w, h, note -> vm.addBodyMetric(w, h, note); showMetricDialog = false }
                )
            }
        }

        // === WEEKLY VOLUME CHART ===
        item {
            StatsCard(title = "Weekly Volume", icon = Icons.Filled.BarChart) {
                if (state.weeklyVolume.isNotEmpty()) {
                    BarChart(
                        data = state.weeklyVolume.reversed().takeLast(8),
                        labelFn = { it.weekStart.takeLast(2) },
                        valueFn = { it.totalVolume }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Total kg lifted per minggu (weight × reps)", color = TextTertiary, fontSize = 10.sp)
                } else {
                    Text("Belum ada data workout.", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        // === MONTHLY PROGRESS ===
        item {
            StatsCard(title = "Monthly Progress", icon = Icons.Filled.TrendingUp) {
                if (state.monthlyVolume.isNotEmpty()) {
                    MonthlyBarChart(state.monthlyVolume.reversed())
                    Spacer(Modifier.height(8.dp))
                    state.monthlyVolume.take(4).forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.month, color = TextSecondary, fontSize = 12.sp)
                            Text("${m.sessions} sesi • ${m.totalSets} set • ${"%.0f".format(m.totalVolume)} kg", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text("Belum ada data bulanan.", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        // === ACTIVITY HEATMAP (GitHub-style) ===
        item {
            StatsCard(title = "Activity (90 days)", icon = Icons.Filled.CalendarMonth) {
                ActivityHeatmap(state.activityHeatmap)
            }
        }

        // === CARDIO LOGS ===
        item {
            var showCardioDialog by remember { mutableStateOf(false) }
            StatsCard(
                title = "Cardio & Calorie Burn",
                icon = Icons.Filled.DirectionsRun,
                action = { showCardioDialog = true }
            ) {
                if (state.cardioLogs.isNotEmpty()) {
                    state.cardioLogs.take(10).forEach { log ->
                        CardioRow(log) { vm.deleteCardioLog(log.id) }
                    }
                } else {
                    Text("Belum ada cardio log. Tap + untuk tambah.", color = TextTertiary, fontSize = 12.sp)
                }
            }
            if (showCardioDialog) {
                AddCardioDialog(
                    onDismiss = { showCardioDialog = false },
                    onSave = { type, dur, cal, dist, note ->
                        vm.addCardioLog(type, dur, cal, dist, note)
                        showCardioDialog = false
                    }
                )
            }
        }

        // === ENERGY BURNED SUMMARY ===
        item {
            val total = state.totalCardioCalories + state.estimatedWorkoutCalories
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(AccentPrimary.copy(alpha = 0.15f), AccentCyan.copy(alpha = 0.15f)))
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Energy Burned", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("$total kcal", color = AccentCyan, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = AccentPink, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

// ─── Composable helpers ───

@Composable
private fun OverviewCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, color = TextTertiary, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(" $unit", color = TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (action != null) {
                IconButton(onClick = action, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = AccentPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun WeightLineChart(metrics: List<BodyMetric>) {
    val weights = metrics.map { it.weightKg }
    val minW = weights.min()
    val maxW = weights.max()
    val range = (maxW - minW).coerceAtLeast(0.1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val w = size.width
        val h = size.height
        val stepX = if (weights.size > 1) w / (weights.size - 1) else w

        val path = Path()
        weights.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (h - ((v - minW) / range) * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = AccentCyan,
            style = Stroke(width = 3.dp.toPx())
        )

        // Dots
        weights.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (h - ((v - minW) / range) * h).toFloat()
            drawCircle(color = AccentCyan, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(metrics.first().date.takeLast(5), color = TextTertiary, fontSize = 9.sp)
        Text(metrics.last().date.takeLast(5), color = TextTertiary, fontSize = 9.sp)
    }
}

@Composable
private fun BarChart(
    data: List<com.phenom.gym.data.local.gym.WeeklyVolume>,
    labelFn: (com.phenom.gym.data.local.gym.WeeklyVolume) -> String,
    valueFn: (com.phenom.gym.data.local.gym.WeeklyVolume) -> Double
) {
    if (data.isEmpty()) return
    val values = data.map { valueFn(it) }
    val maxV = values.max().coerceAtLeast(1.0)

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        data.forEach { item ->
            val v = valueFn(item)
            val ratio = (v / maxV).toFloat()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val barW = size.width * 0.7f
                    val barH = size.height * ratio
                    val x = (size.width - barW) / 2
                    val y = size.height - barH
                    drawRoundRect(
                        color = AccentPrimary,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                Text(labelFn(item), color = TextTertiary, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MonthlyBarChart(data: List<com.phenom.gym.data.local.gym.MonthlyVolume>) {
    if (data.isEmpty()) return
    val maxV = data.maxOf { it.totalVolume }.coerceAtLeast(1.0)

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { item ->
            val ratio = (item.totalVolume / maxV).toFloat()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    val barW = size.width * 0.6f
                    val barH = size.height * ratio
                    val x = (size.width - barW) / 2
                    val y = size.height - barH
                    drawRoundRect(
                        color = AccentCyan,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                Text(item.month.takeLast(2), color = TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ActivityHeatmap(days: List<ActivityDay>) {
    // Group into weeks (columns of 7)
    val weeks = days.chunked(7)
    val heatColors = listOf(
        BgCard,                              // 0 = no activity
        AccentPrimary.copy(alpha = 0.25f),   // 1
        AccentPrimary.copy(alpha = 0.50f),   // 2
        AccentPrimary.copy(alpha = 0.75f),   // 3
        AccentCyan                           // 4
    )

    Column {
        // Heatmap grid
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weeks.forEach { week ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatColors[day.intensity])
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", color = TextTertiary, fontSize = 9.sp)
            Spacer(Modifier.width(4.dp))
            heatColors.forEach { c ->
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                Spacer(Modifier.width(2.dp))
            }
            Text("More", color = TextTertiary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun CardioRow(log: CardioLog, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${log.type} • ${log.date.takeLast(5)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${log.durationMin} min • ${log.caloriesBurned} kcal${if (log.distanceKm > 0) " • ${log.distanceKm} km" else ""}", color = TextTertiary, fontSize = 11.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Dialogs ───

@Composable
private fun AddMetricDialog(onDismiss: () -> Unit, onSave: (Double, Double, String) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Body Metric", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                GymInput(weight, { weight = it }, "Weight (kg)", Modifier.fillMaxWidth(), KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                GymInput(height, { height = it }, "Height (cm)", Modifier.fillMaxWidth(), KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                GymInput(note, { note = it }, "Note (optional)", Modifier.fillMaxWidth(), KeyboardType.Text)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                if (w > 0) onSave(w, h, note)
            }) { Text("Save", color = AccentGreen, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardioDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Double, String) -> Unit
) {
    var type by remember { mutableStateOf(GymRepository.CARDIO_PRESETS.first()) }
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("") }
    var incline by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Cardio", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Type dropdown — ExposedDropdownMenuBox (responsive click)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan, unfocusedBorderColor = BorderSubtle,
                            focusedLabelColor = AccentCyan, unfocusedLabelColor = TextTertiary,
                            cursorColor = AccentCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        GymRepository.CARDIO_PRESETS.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p, color = TextPrimary) },
                                onClick = { type = p; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                GymInput(duration, { duration = it }, "Duration (min)", Modifier.fillMaxWidth(), KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                GymInput(calories, { calories = it }, "Calories burned", Modifier.fillMaxWidth(), KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymInput(speed, { speed = it }, "Speed (km/h)", Modifier.weight(1f), KeyboardType.Decimal)
                    GymInput(incline, { incline = it }, "Incline (%)", Modifier.weight(1f), KeyboardType.Decimal)
                }
                Spacer(Modifier.height(8.dp))
                GymInput(distance, { distance = it }, "Distance (km, optional)", Modifier.fillMaxWidth(), KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                GymInput(note, { note = it }, "Note (optional)", Modifier.fillMaxWidth(), KeyboardType.Text)

                // Auto-fill note from speed/incline if user leaves note empty
                if (speed.isNotEmpty() || incline.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            val parts = mutableListOf<String>()
                            if (speed.isNotEmpty()) parts += "${speed} km/h"
                            if (incline.isNotEmpty()) parts += "incline ${incline}%"
                            if (parts.isNotEmpty()) append("Auto-note: " + parts.joinToString(" · "))
                        },
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dur = duration.toIntOrNull() ?: 0
                val cal = calories.toIntOrNull() ?: 0
                val dist = distance.toDoubleOrNull() ?: 0.0
                // Build note: user note + speed/incline info
                val parts = mutableListOf<String>()
                if (speed.isNotEmpty()) parts += "${speed} km/h"
                if (incline.isNotEmpty()) parts += "incline ${incline}%"
                if (note.isNotEmpty()) parts += note
                val finalNote = parts.joinToString(" · ")
                if (dur > 0 || cal > 0) onSave(type, dur, cal, dist, finalNote)
            }) { Text("Save", color = AccentGreen, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}
