package com.phenom.gym.ui.gym

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phenom.gym.data.local.gym.ActivityDay
import com.phenom.gym.data.local.gym.BodyMetric
import com.phenom.gym.data.local.gym.CardioLog
import com.phenom.gym.data.local.gym.MonthlyVolume
import com.phenom.gym.data.local.gym.WeeklyVolume
import com.phenom.gym.data.repo.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsState(
    val bodyMetrics: List<BodyMetric> = emptyList(),
    val latestWeight: Double? = null,
    val latestHeight: Double? = null,
    val bmi: Double? = null,
    val bmiCategory: String = "",
    val cardioLogs: List<CardioLog> = emptyList(),
    val weeklyVolume: List<WeeklyVolume> = emptyList(),
    val monthlyVolume: List<MonthlyVolume> = emptyList(),
    val activityHeatmap: List<ActivityDay> = emptyList(),
    val totalCardioCalories: Int = 0,
    val estimatedWorkoutCalories: Int = 0,
    val streak: Int = 0,
    val isLoading: Boolean = false
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GymRepository(app)

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init { refreshAll() }

    fun refreshAll() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val metrics = repo.getAllBodyMetrics()
            val latest = repo.getLatestBodyMetric()
            val cardio = repo.getAllCardioLogs()
            val weekly = repo.getWeeklyVolume(12)
            val monthly = repo.getMonthlyVolume(6)
            val heatmap = repo.getActivityHeatmap(90)
            val cardioCal = repo.getTotalCardioCalories()
            val workoutCal = repo.getEstimatedWorkoutCalories()
            val streak = repo.getWorkoutStreak()

            val bmi = if (latest != null && latest.heightCm > 0) {
                val h = latest.heightCm / 100.0
                latest.weightKg / (h * h)
            } else null
            val bmiCat = when {
                bmi == null -> ""
                bmi < 18.5 -> "Underweight"
                bmi < 25.0 -> "Normal"
                bmi < 30.0 -> "Overweight"
                else -> "Obese"
            }

            _state.update {
                it.copy(
                    bodyMetrics = metrics,
                    latestWeight = latest?.weightKg,
                    latestHeight = latest?.heightCm?.takeIf { h -> h > 0 },
                    bmi = bmi,
                    bmiCategory = bmiCat,
                    cardioLogs = cardio,
                    weeklyVolume = weekly,
                    monthlyVolume = monthly,
                    activityHeatmap = heatmap,
                    totalCardioCalories = cardioCal,
                    estimatedWorkoutCalories = workoutCal,
                    streak = streak,
                    isLoading = false
                )
            }
        }
    }

    fun addBodyMetric(weightKg: Double, heightCm: Double, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = repo.todayIso()
            repo.insertBodyMetric(
                BodyMetric(date = today, weightKg = weightKg, heightCm = heightCm, note = note)
            )
            refreshAll()
        }
    }

    fun deleteBodyMetric(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteBodyMetric(id)
            refreshAll()
        }
    }

    fun addCardioLog(type: String, durationMin: Int, calories: Int, distanceKm: Double, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = repo.todayIso()
            repo.insertCardioLog(
                CardioLog(
                    date = today, type = type, durationMin = durationMin,
                    caloriesBurned = calories, distanceKm = distanceKm, note = note
                )
            )
            refreshAll()
        }
    }

    fun deleteCardioLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteCardioLog(id)
            refreshAll()
        }
    }
}
