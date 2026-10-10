package com.phenom.gym.data.repo

import android.content.Context
import com.phenom.gym.data.local.gym.ActivityDay
import com.phenom.gym.data.local.gym.BodyMetric
import com.phenom.gym.data.local.gym.CardioLog
import com.phenom.gym.data.local.gym.Exercise
import com.phenom.gym.data.local.gym.GymDbHelper
import com.phenom.gym.data.local.gym.MonthlyVolume
import com.phenom.gym.data.local.gym.WeeklyVolume
import com.phenom.gym.data.local.gym.WorkoutDay
import com.phenom.gym.data.local.gym.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GymRepository(context: Context) {

    private val db = GymDbHelper(context)

    suspend fun getAllDays(): List<WorkoutDay> = withContext(Dispatchers.IO) { db.queryAllDays() }
    suspend fun getDaysByDayOfWeek(dow: Int): List<WorkoutDay> = withContext(Dispatchers.IO) { db.queryDaysByDayOfWeek(dow) }
    suspend fun insertDay(day: WorkoutDay): Long = withContext(Dispatchers.IO) { db.insertDay(day) }
    suspend fun deleteDay(id: Long) = withContext(Dispatchers.IO) { db.deleteDay(id) }

    suspend fun getExercisesForDay(dayId: Long): List<Exercise> = withContext(Dispatchers.IO) { db.queryExercisesForDay(dayId) }
    suspend fun insertExercise(ex: Exercise): Long = withContext(Dispatchers.IO) { db.insertExercise(ex) }
    suspend fun deleteExercise(id: Long) = withContext(Dispatchers.IO) { db.deleteExercise(id) }

    suspend fun getLogsForExercise(exerciseId: Long, date: String): List<WorkoutLog> = withContext(Dispatchers.IO) { db.queryLogsForExercise(exerciseId, date) }
    suspend fun getAllLogsForExercise(exerciseId: Long): List<WorkoutLog> = withContext(Dispatchers.IO) { db.queryAllLogsForExercise(exerciseId) }
    suspend fun insertLog(log: WorkoutLog): Long = withContext(Dispatchers.IO) { db.insertLog(log) }
    suspend fun deleteLog(id: Long) = withContext(Dispatchers.IO) { db.deleteLog(id) }
    suspend fun getRecentDates(limit: Int = 30): List<String> = withContext(Dispatchers.IO) { db.queryRecentDates(limit) }
    suspend fun getCompletedCountForDate(date: String): Int = withContext(Dispatchers.IO) { db.queryCompletedCountForDate(date) }

    // ─── Body Metrics ───
    suspend fun insertBodyMetric(m: BodyMetric): Long = withContext(Dispatchers.IO) { db.insertBodyMetric(m) }
    suspend fun getAllBodyMetrics(): List<BodyMetric> = withContext(Dispatchers.IO) { db.queryAllBodyMetrics() }
    suspend fun getLatestBodyMetric(): BodyMetric? = withContext(Dispatchers.IO) { db.queryLatestBodyMetric() }
    suspend fun deleteBodyMetric(id: Long) = withContext(Dispatchers.IO) { db.deleteBodyMetric(id) }

    // ─── Cardio Logs ───
    suspend fun insertCardioLog(log: CardioLog): Long = withContext(Dispatchers.IO) { db.insertCardioLog(log) }
    suspend fun getAllCardioLogs(): List<CardioLog> = withContext(Dispatchers.IO) { db.queryAllCardioLogs() }
    suspend fun getCardioForDate(date: String): List<CardioLog> = withContext(Dispatchers.IO) { db.queryCardioForDate(date) }
    suspend fun deleteCardioLog(id: Long) = withContext(Dispatchers.IO) { db.deleteCardioLog(id) }

    // ─── Aggregated Stats ───
    suspend fun getWeeklyVolume(weeks: Int = 12): List<WeeklyVolume> = withContext(Dispatchers.IO) { db.queryWeeklyVolume(weeks) }
    suspend fun getMonthlyVolume(months: Int = 6): List<MonthlyVolume> = withContext(Dispatchers.IO) { db.queryMonthlyVolume(months) }
    suspend fun getActivityHeatmap(days: Int = 90): List<ActivityDay> = withContext(Dispatchers.IO) { db.queryActivityHeatmap(days) }
    suspend fun getTotalCardioCalories(): Int = withContext(Dispatchers.IO) { db.queryTotalCardioCalories() }
    suspend fun getEstimatedWorkoutCalories(): Int = withContext(Dispatchers.IO) { db.queryEstimatedWorkoutCalories() }
    suspend fun getWorkoutStreak(): Int = withContext(Dispatchers.IO) { db.queryWorkoutStreak() }

    // ─── Import / Export ───
    fun exportToJson(): String = db.exportToJson()
    suspend fun importFromJson(jsonStr: String): Int = withContext(Dispatchers.IO) { db.importFromJson(jsonStr) }

    // Helpers
    fun todayIso(): String = LocalDate.now().toString()
    fun todayDayOfWeek(): Int = LocalDate.now().dayOfWeek.value
    fun dayName(dayOfWeek: Int): String {
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        return days.getOrElse(dayOfWeek - 1) { "Unknown" }
    }
    fun todayDayName(): String = dayName(todayDayOfWeek())

    companion object {
        val EXERCISE_PRESETS = listOf(
            "Lat Pulldown", "Seated Cable Row", "Hip Abduction", "Hip Thrust",
            "Leg Press", "Romanian Deadlift", "Cable Kickback", "Chest Press",
            "Lateral Raise", "Leg Curl", "Leg Extension", "Hack Squat",
            "Goblet Squat", "Bulgarian Split Squat", "Walking Lunge",
            "Face Pull", "Reverse Crunch", "Plank",
            "Bench Press", "Incline Dumbbell Press", "Pull Up", "Chin Up",
            "Barbell Row", "Deadlift", "Squat", "Front Squat",
            "Hammer Curl", "Bicep Curl", "Tricep Pushdown", "Skull Crusher",
            "Shoulder Press", "Rear Delt Fly", "Calf Raise",
            "Hanging Leg Raise", "Cable Crunch", "Russian Twist"
        )

        val CARDIO_PRESETS = listOf(
            "Walking (Incline)", "Walking", "Running", "Cycling", "Jump Rope", "Swimming",
            "Rowing", "Elliptical", "Stair Climber", "HIIT", "Other"
        )
    }
}
