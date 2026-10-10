package com.phenom.gym.ui.gym

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.data.local.gym.Exercise
import com.phenom.gym.data.local.gym.WorkoutDay
import com.phenom.gym.data.local.gym.WorkoutLog
import com.phenom.gym.data.repo.GymRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GymViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GymRepository(app)

    val todayDayOfWeek = repo.todayDayOfWeek()
    val todayDayName = repo.todayDayName()
    val todayIso = repo.todayIso()

    private val _allDays = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val allDays: StateFlow<List<WorkoutDay>> = _allDays.asStateFlow()

    private val _todayDays = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val todayDays: StateFlow<List<WorkoutDay>> = _todayDays.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayIso)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _recentDates = MutableStateFlow<List<String>>(emptyList())
    val recentDates: StateFlow<List<String>> = _recentDates.asStateFlow()

    private val _completedToday = MutableStateFlow(0)
    val completedToday: StateFlow<Int> = _completedToday.asStateFlow()

    // Cache exercises per dayId
    private val _exercisesCache = MutableStateFlow<Map<Long, List<Exercise>>>(emptyMap())
    val exercisesCache: StateFlow<Map<Long, List<Exercise>>> = _exercisesCache.asStateFlow()

    // Cache logs per exerciseId+date
    private val _logsCache = MutableStateFlow<Map<String, List<WorkoutLog>>>(emptyMap())
    val logsCache: StateFlow<Map<String, List<WorkoutLog>>> = _logsCache.asStateFlow()

    init { refreshAll() }

    fun refreshAll() {
        viewModelScope.launch {
            _allDays.value = repo.getAllDays()
            _todayDays.value = repo.getDaysByDayOfWeek(todayDayOfWeek)
            _recentDates.value = repo.getRecentDates(30)
            _completedToday.value = repo.getCompletedCountForDate(_selectedDate.value)
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            _completedToday.value = repo.getCompletedCountForDate(date)
            // Re-fetch logs for all exercises of today's days
            _allDays.value.forEach { day ->
                loadExercises(day.id)
            }
        }
    }

    fun addDay(dayOfWeek: Int, title: String) {
        viewModelScope.launch {
            val maxOrder = _allDays.value.filter { it.dayOfWeek == dayOfWeek }.maxOfOrNull { it.sortOrder } ?: -1
            repo.insertDay(WorkoutDay(
                dayOfWeek = dayOfWeek,
                dayName = repo.dayName(dayOfWeek),
                title = title,
                sortOrder = maxOrder + 1
            ))
            refreshAll()
        }
    }

    fun deleteDay(day: WorkoutDay) {
        viewModelScope.launch {
            repo.deleteDay(day.id)
            refreshAll()
        }
    }

    fun loadExercises(dayId: Long) {
        viewModelScope.launch {
            val list = repo.getExercisesForDay(dayId)
            _exercisesCache.value = _exercisesCache.value + (dayId to list)
            // Also load logs for each exercise on selected date
            list.forEach { ex ->
                loadLogs(ex.id, _selectedDate.value)
            }
        }
    }

    fun addExercise(dayId: Long, name: String, targetSets: Int = 3, targetReps: Int = 10, category: String = "") {
        viewModelScope.launch {
            repo.insertExercise(Exercise(
                workoutDayId = dayId, name = name, category = category,
                targetSets = targetSets, targetReps = targetReps
            ))
            loadExercises(dayId)
        }
    }

    fun deleteExercise(ex: Exercise) {
        viewModelScope.launch {
            repo.deleteExercise(ex.id)
            loadExercises(ex.workoutDayId)
        }
    }

    fun loadLogs(exerciseId: Long, date: String) {
        viewModelScope.launch {
            val key = "$exerciseId|$date"
            val list = repo.getLogsForExercise(exerciseId, date)
            _logsCache.value = _logsCache.value + (key to list)
        }
    }

    fun saveLog(exerciseId: Long, date: String, setNumber: Int, reps: Int, weight: Double, completed: Boolean) {
        viewModelScope.launch {
            repo.insertLog(WorkoutLog(
                exerciseId = exerciseId, date = date, setNumber = setNumber,
                reps = reps, weight = weight, completed = completed
            ))
            loadLogs(exerciseId, date)
            _completedToday.value = repo.getCompletedCountForDate(_selectedDate.value)
        }
    }

    // ─── Import / Export ───
    private val _importExportStatus = MutableStateFlow<String?>(null)
    val importExportStatus: StateFlow<String?> = _importExportStatus.asStateFlow()

    fun exportWorkout(): String {
        return repo.exportToJson().also {
            _importExportStatus.value = "Export berhasil — ${it.length} bytes"
        }
    }

    fun importWorkout(jsonStr: String) {
        viewModelScope.launch {
            try {
                val count = repo.importFromJson(jsonStr)
                _importExportStatus.value = "Import berhasil — $count hari diimpor"
                refreshAll()
            } catch (e: Exception) {
                _importExportStatus.value = "Import gagal: ${e.message}"
            }
        }
    }

    fun clearImportExportStatus() {
        _importExportStatus.value = null
    }

    fun deleteLog(log: WorkoutLog) {
        viewModelScope.launch {
            repo.deleteLog(log.id)
            loadLogs(log.exerciseId, log.date)
            _completedToday.value = repo.getCompletedCountForDate(log.date)
        }
    }

    fun getLogs(exerciseId: Long, date: String): List<WorkoutLog> {
        val key = "$exerciseId|$date"
        return _logsCache.value[key] ?: emptyList()
    }

    val exercisePresets = GymRepository.EXERCISE_PRESETS
}
