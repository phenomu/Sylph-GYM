package com.phenom.gym.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phenom.gym.data.local.gym.Exercise
import com.phenom.gym.data.local.gym.WorkoutDay
import com.phenom.gym.data.local.gym.WorkoutLog
import com.phenom.gym.ui.theme.*

@Composable
fun GymScreen(vm: GymViewModel = viewModel()) {
    var showStats by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BgDark)) {
        // Toggle Workout / Statistics
        WorkoutStatsToggle(showStats) { showStats = it }

        if (showStats) {
            StatsScreen()
        } else {
            GymScreenContent(vm)
        }
    }
}

@Composable
private fun WorkoutStatsToggle(selectedStats: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToggleChip("Workout", !selectedStats, Modifier.weight(1f)) { onToggle(false) }
        ToggleChip("Statistics", selectedStats, Modifier.weight(1f)) { onToggle(true) }
    }
}

@Composable
private fun ToggleChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AccentPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) AccentCyan else TextTertiary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun GymScreenContent(vm: GymViewModel) {
    val todayDays by vm.todayDays.collectAsState()
    val allDays by vm.allDays.collectAsState()
    val todayName = vm.todayDayName
    val todayIso = vm.todayIso
    val selectedDate by vm.selectedDate.collectAsState()
    val completedToday by vm.completedToday.collectAsState()
    val recentDates by vm.recentDates.collectAsState()
    val exercisesCache by vm.exercisesCache.collectAsState()
    val logsCache by vm.logsCache.collectAsState()

    val otherDays = allDays.filter { it.dayOfWeek != vm.todayDayOfWeek }

    var showAddDayDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(todayDays) { todayDays.forEach { vm.loadExercises(it.id) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgDark).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        // === HEADER ===
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Workout", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("$todayName • $todayIso", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Completed", "$completedToday", AccentGreen, Modifier.weight(1f))
                    DateCard(selectedDate.takeLast(5), Modifier.weight(1f)) { showDatePicker = true }
                }
            }
        }

        // === TODAY ===
        if (todayDays.isEmpty()) {
            item { EmptyCard("No workout today", "Tambah jadwal untuk $todayName") { showAddDayDialog = true } }
        } else {
            item { SectionLabel("Today's Schedule") }
            items(todayDays) { day -> WorkoutDayCard(day, vm, true, exercisesCache, logsCache, selectedDate) }
        }

        // === OTHER DAYS ===
        if (otherDays.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); SectionLabel("Other Days") }
            items(otherDays) { day -> WorkoutDayCard(day, vm, false, exercisesCache, logsCache, selectedDate) }
        }

        // === HISTORY ===
        if (recentDates.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)); SectionLabel("Recent History") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentDates.take(10)) { date ->
                        Chip(date, if (date == selectedDate) AccentGreen else AccentCyan) { vm.setSelectedDate(date) }
                    }
                }
            }
        }

        // === ADD DAY ===
        item {
            PrimaryButton("+ Add Schedule", AccentPrimary) { showAddDayDialog = true }
        }

        // === IMPORT / EXPORT ===
        item {
            val status by vm.importExportStatus.collectAsState()
            val context = LocalContext.current

            Spacer(Modifier.height(4.dp))
            SectionLabel("Share Workout")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export
                OutlinedButton(
                    onClick = {
                        val json = vm.exportWorkout()
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(android.content.Intent.EXTRA_TEXT, json)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Sylph GYM Workout Export.json")
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan workout via..."))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AccentCyan.copy(alpha = 0.1f),
                        contentColor = AccentCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                // Import
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AccentPrimary.copy(alpha = 0.1f),
                        contentColor = AccentPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            // Status message
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    status!!,
                    color = if (status!!.startsWith("Import berhasil") || status!!.startsWith("Export berhasil"))
                        AccentGreen else AccentOrange,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showAddDayDialog) { AddDayDialog({ showAddDayDialog = false }) { dow, title -> vm.addDay(dow, title); showAddDayDialog = false } }
    if (showDatePicker) { DatePickerDialog({ showDatePicker = false }) { vm.setSelectedDate(it); showDatePicker = false } }
    if (showImportDialog) {
        ImportJsonDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                vm.importWorkout(json)
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DateCard(date: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text("Date", color = TextSecondary, fontSize = 11.sp)
            Text(date, color = AccentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WorkoutDayCard(day: WorkoutDay, vm: GymViewModel, isToday: Boolean, exercisesCache: Map<Long, List<Exercise>>, logsCache: Map<String, List<WorkoutLog>>, selectedDate: String) {
    var expanded by remember { mutableStateOf(isToday) }
    val accent = if (isToday) AccentGreen else AccentCyan
    val exercises = exercisesCache[day.id] ?: emptyList()

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(BgCard)
            .border(1.dp, if (isToday) accent.copy(alpha = 0.4f) else BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded = !expanded }.weight(1f)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(day.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(day.dayName, color = accent, fontSize = 11.sp)
                }
            }
            Row {
                IconButton(onClick = { vm.deleteDay(day) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp)) }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Expand", tint = TextTertiary, modifier = Modifier.size(20.dp).clickable { expanded = !expanded })
            }
        }

        if (expanded) {
            LaunchedEffect(day.id) { vm.loadExercises(day.id) }
            Spacer(Modifier.height(12.dp))
            if (exercises.isEmpty()) {
                Text("Belum ada exercise. Tap tombol di bawah untuk tambah.", color = TextTertiary, fontSize = 12.sp)
            } else {
                exercises.forEach { ex -> ExerciseRow(ex, vm, selectedDate, logsCache); Spacer(Modifier.height(8.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            var showAddEx by remember { mutableStateOf(false) }
            SecondaryButton("+ Add Exercise", AccentCyan) { showAddEx = true }
            if (showAddEx) { AddExerciseDialog({ showAddEx = false }, { name, sets, reps -> vm.addExercise(day.id, name, sets, reps); showAddEx = false }, vm.exercisePresets) }
        }
    }
}

@Composable
private fun ExerciseRow(ex: Exercise, vm: GymViewModel, date: String, logsCache: Map<String, List<WorkoutLog>>) {
    var expanded by remember { mutableStateOf(false) }
    val key = "${ex.id}|$date"
    val logs = logsCache[key] ?: emptyList()
    val completedCount = logs.count { it.completed }
    val isDone = completedCount >= ex.targetSets

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BgInput)
            .border(1.dp, if (isDone) AccentGreen.copy(alpha = 0.3f) else BorderSubtle, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }.padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Target ${ex.targetSets}×${ex.targetReps} • Done $completedCount/${ex.targetSets}", color = if (isDone) AccentGreen else TextTertiary, fontSize = 11.sp)
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            logs.forEach { log -> LogRow(log, vm) }
            var showAddSet by remember { mutableStateOf(false) }
            Spacer(Modifier.height(4.dp))
            SecondaryButton("+ Add Set", AccentPrimary, compact = true) { showAddSet = true }
            if (showAddSet) { AddSetDialog(logs.size + 1, { showAddSet = false }) { reps, weight -> vm.saveLog(ex.id, date, logs.size + 1, reps, weight, true); showAddSet = false } }
        }
    }
}

@Composable
private fun LogRow(log: WorkoutLog, vm: GymViewModel) {
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        var reps by remember { mutableStateOf(log.reps.toString()) }
        var weight by remember { mutableStateOf(log.weight.toString()) }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Set ${log.setNumber}", color = AccentYellow, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(56.dp))
            GymInput(reps, { reps = it }, "reps", Modifier.weight(1f), KeyboardType.Number)
            Spacer(Modifier.width(6.dp))
            GymInput(weight, { weight = it }, "kg", Modifier.weight(1f), KeyboardType.Decimal)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { vm.saveLog(log.exerciseId, log.date, log.setNumber, reps.toIntOrNull() ?: 0, weight.toDoubleOrNull() ?: 0.0, true); editing = false }) { Icon(Icons.Filled.Check, contentDescription = "Save", tint = AccentGreen, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { editing = false }) { Icon(Icons.Filled.ExpandLess, contentDescription = "Cancel", tint = TextTertiary, modifier = Modifier.size(18.dp)) }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { editing = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set ${log.setNumber}: ${log.reps} reps × ${log.weight}kg", color = if (log.completed) TextPrimary else TextTertiary, fontSize = 12.sp)
            Row {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(AccentOrange.copy(alpha = 0.1f)).clickable { vm.deleteLog(log) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentOrange, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BgCard).border(1.dp, BorderSubtle, RoundedCornerShape(20.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = TextTertiary, fontSize = 13.sp)
    }
}

@Composable
private fun PrimaryButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.8f))))
            .clickable(onClick = onClick).padding(14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SecondaryButton(text: String, color: Color, compact: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun Chip(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun GymInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, fontSize = 12.sp, color = TextTertiary) },
        modifier = modifier, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentPrimary, unfocusedBorderColor = BorderSubtle,
            cursorColor = AccentPrimary, focusedContainerColor = BgInput, unfocusedContainerColor = BgInput,
            focusedPlaceholderColor = TextTertiary, unfocusedPlaceholderColor = TextTertiary
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = TextPrimary)
    )
}

// ===== DIALOGS =====
@Composable
private fun AddDayDialog(onDismiss: () -> Unit, onAdd: (Int, String) -> Unit) {
    var selectedDay by remember { mutableStateOf(1) }
    var title by remember { mutableStateOf("") }
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Add Schedule", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Day", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(days) { idx, d -> Chip(d, if (idx + 1 == selectedDay) AccentGreen else AccentCyan) { selectedDay = idx + 1 } }
                }
                Spacer(Modifier.height(16.dp))
                Text("Title", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                GymInput(title, { title = it }, "e.g. Push Day, Leg Day...", Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onAdd(selectedDay, title.trim()) }) { Text("Save", color = AccentGreen, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}

@Composable
private fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (String, Int, Int) -> Unit, presets: List<String>) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var showPresets by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Add Exercise", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (showPresets) {
                    Text("Pilih preset:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(presets) { p ->
                            Text(p, color = AccentCyan, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { name = p; showPresets = false }.padding(vertical = 6.dp))
                        }
                    }
                } else {
                    TextButton(onClick = { showPresets = true }) { Text("Choose Preset →", color = AccentCyan, fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                    GymInput(name, { name = it }, "Exercise name", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymInput(sets, { sets = it }, "Sets", Modifier.weight(1f), KeyboardType.Number)
                        GymInput(reps, { reps = it }, "Reps", Modifier.weight(1f), KeyboardType.Number)
                    }
                }
            }
        },
        confirmButton = {
            if (!showPresets) TextButton(onClick = {
                val s = sets.toIntOrNull() ?: 3; val r = reps.toIntOrNull() ?: 10
                if (name.isNotBlank()) onAdd(name.trim(), s, r)
            }) { Text("Add", color = AccentGreen, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}

@Composable
private fun AddSetDialog(setNumber: Int, onDismiss: () -> Unit, onSave: (Int, Double) -> Unit) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Set $setNumber", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                GymInput(reps, { reps = it }, "Reps", Modifier.fillMaxWidth(), KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                GymInput(weight, { weight = it }, "Weight (kg)", Modifier.fillMaxWidth(), KeyboardType.Decimal)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(reps.toIntOrNull() ?: 0, weight.toDoubleOrNull() ?: 0.0) }) { Text("Save", color = AccentGreen, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}

@Composable
private fun DatePickerDialog(onDismiss: () -> Unit, onDateSelected: (String) -> Unit) {
    val dates = remember { (0..30).map { java.time.LocalDate.now().minusDays(it.toLong()).toString() } }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Select Date", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(dates) { d ->
                    Text(d, color = AccentCyan, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { onDateSelected(d) }.padding(vertical = 8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextTertiary) } }
    )
}

@Composable
private fun ImportJsonDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard, shape = RoundedCornerShape(20.dp),
        title = { Text("Import Workout", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Tempel JSON workout di bawah ini. Kamu bisa dapatkan JSON dari teman yang share workout via Export.",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Tempel JSON di sini...", color = TextTertiary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = AccentPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onImport(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Import", color = if (text.isNotBlank()) AccentGreen else TextTertiary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) } }
    )
}
