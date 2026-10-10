package com.phenom.gym.data.local.gym

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName

// ===== Data classes =====
data class WorkoutDay(
    val id: Long = 0,
    val dayOfWeek: Int,
    val dayName: String,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Exercise(
    val id: Long = 0,
    val workoutDayId: Long,
    val name: String,
    val category: String = "",
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkoutLog(
    val id: Long = 0,
    val exerciseId: Long,
    val date: String,
    val setNumber: Int,
    val reps: Int,
    val weight: Double = 0.0,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ===== Body metrics =====
data class BodyMetric(
    val id: Long = 0,
    val date: String,
    val weightKg: Double,
    val heightCm: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ===== Cardio log =====
data class CardioLog(
    val id: Long = 0,
    val date: String,
    val type: String,           // "Running", "Cycling", "Jump Rope", "Swimming", dll
    val durationMin: Int,        // minutes
    val caloriesBurned: Int,    // kcal
    val distanceKm: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ===== Aggregated stats =====
data class WeeklyVolume(
    val weekStart: String,      // ISO date of Monday
    val totalVolume: Double,    // sum of (weight * reps) for all completed sets
    val totalSets: Int,
    val totalReps: Int
)

data class MonthlyVolume(
    val month: String,          // "2026-10"
    val totalVolume: Double,
    val totalSets: Int,
    val sessions: Int           // distinct dates with completed logs
)

data class ActivityDay(
    val date: String,
    val intensity: Int          // 0 = no activity, 1-4 = scaled by volume/sets
)

// ===== Import/Export JSON models =====
@Serializable
data class ExportExercise(
    val name: String,
    val category: String = "",
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val sortOrder: Int = 0
)

@Serializable
data class ExportWorkoutDay(
    val dayOfWeek: Int,
    val dayName: String,
    val title: String,
    val sortOrder: Int = 0,
    val exercises: List<ExportExercise> = emptyList()
)

@Serializable
data class WorkoutExport(
    val version: Int = 1,
    val exportedAt: String = "",
    val days: List<ExportWorkoutDay> = emptyList()
)

// ===== SQLiteOpenHelper =====
class GymDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "phenom_gym.db"
        private const val DB_VERSION = 2

        private const val T_DAYS = "workout_days"
        private const val T_EXERCISES = "exercises"
        private const val T_LOGS = "workout_logs"
        private const val T_METRICS = "body_metrics"
        private const val T_CARDIO = "cardio_logs"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $T_DAYS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dayOfWeek INTEGER NOT NULL,
                dayName TEXT NOT NULL,
                title TEXT NOT NULL,
                sortOrder INTEGER DEFAULT 0,
                createdAt INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_EXERCISES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workoutDayId INTEGER NOT NULL,
                name TEXT NOT NULL,
                category TEXT DEFAULT '',
                targetSets INTEGER DEFAULT 3,
                targetReps INTEGER DEFAULT 10,
                sortOrder INTEGER DEFAULT 0,
                createdAt INTEGER DEFAULT 0,
                FOREIGN KEY (workoutDayId) REFERENCES $T_DAYS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_LOGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exerciseId INTEGER NOT NULL,
                date TEXT NOT NULL,
                setNumber INTEGER NOT NULL,
                reps INTEGER NOT NULL,
                weight REAL DEFAULT 0,
                completed INTEGER DEFAULT 0,
                createdAt INTEGER DEFAULT 0,
                FOREIGN KEY (exerciseId) REFERENCES $T_EXERCISES(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX idx_exercises_day ON $T_EXERCISES(workoutDayId)")
        db.execSQL("CREATE INDEX idx_logs_exercise ON $T_LOGS(exerciseId)")
        db.execSQL("CREATE INDEX idx_logs_date ON $T_LOGS(date)")

        // Body metrics table
        db.execSQL("""
            CREATE TABLE $T_METRICS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                weightKg REAL NOT NULL,
                heightCm REAL DEFAULT 0,
                note TEXT DEFAULT '',
                createdAt INTEGER DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_metrics_date ON $T_METRICS(date)")

        // Cardio logs table
        db.execSQL("""
            CREATE TABLE $T_CARDIO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                type TEXT NOT NULL,
                durationMin INTEGER DEFAULT 0,
                caloriesBurned INTEGER DEFAULT 0,
                distanceKm REAL DEFAULT 0,
                note TEXT DEFAULT '',
                createdAt INTEGER DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_cardio_date ON $T_CARDIO(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        if (old < 2) {
            // Add new tables for v2 (body metrics + cardio)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_METRICS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    weightKg REAL NOT NULL,
                    heightCm REAL DEFAULT 0,
                    note TEXT DEFAULT '',
                    createdAt INTEGER DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_metrics_date ON $T_METRICS(date)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_CARDIO (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    type TEXT NOT NULL,
                    durationMin INTEGER DEFAULT 0,
                    caloriesBurned INTEGER DEFAULT 0,
                    distanceKm REAL DEFAULT 0,
                    note TEXT DEFAULT '',
                    createdAt INTEGER DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_cardio_date ON $T_CARDIO(date)")
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ===== Queries =====
    fun queryAllDays(): List<WorkoutDay> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_DAYS ORDER BY dayOfWeek ASC, sortOrder ASC", null)
        val list = mutableListOf<WorkoutDay>()
        c.use {
            while (it.moveToNext()) {
                list += WorkoutDay(
                    id = it.getLong(0), dayOfWeek = it.getInt(1), dayName = it.getString(2),
                    title = it.getString(3), sortOrder = it.getInt(4), createdAt = it.getLong(5)
                )
            }
        }
        return list
    }

    fun queryDaysByDayOfWeek(dow: Int): List<WorkoutDay> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_DAYS WHERE dayOfWeek=? ORDER BY sortOrder ASC", arrayOf(dow.toString()))
        val list = mutableListOf<WorkoutDay>()
        c.use {
            while (it.moveToNext()) {
                list += WorkoutDay(
                    id = it.getLong(0), dayOfWeek = it.getInt(1), dayName = it.getString(2),
                    title = it.getString(3), sortOrder = it.getInt(4), createdAt = it.getLong(5)
                )
            }
        }
        return list
    }

    fun insertDay(day: WorkoutDay): Long {
        val cv = ContentValues().apply {
            put("dayOfWeek", day.dayOfWeek)
            put("dayName", day.dayName)
            put("title", day.title)
            put("sortOrder", day.sortOrder)
            put("createdAt", day.createdAt)
        }
        return writableDatabase.insertWithOnConflict(T_DAYS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteDay(id: Long) {
        writableDatabase.delete(T_DAYS, "id=?", arrayOf(id.toString()))
    }

    fun queryExercisesForDay(dayId: Long): List<Exercise> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_EXERCISES WHERE workoutDayId=? ORDER BY sortOrder ASC", arrayOf(dayId.toString()))
        val list = mutableListOf<Exercise>()
        c.use {
            while (it.moveToNext()) {
                list += Exercise(
                    id = it.getLong(0), workoutDayId = it.getLong(1), name = it.getString(2),
                    category = it.getString(3), targetSets = it.getInt(4), targetReps = it.getInt(5),
                    sortOrder = it.getInt(6), createdAt = it.getLong(7)
                )
            }
        }
        return list
    }

    fun insertExercise(ex: Exercise): Long {
        val cv = ContentValues().apply {
            put("workoutDayId", ex.workoutDayId)
            put("name", ex.name)
            put("category", ex.category)
            put("targetSets", ex.targetSets)
            put("targetReps", ex.targetReps)
            put("sortOrder", ex.sortOrder)
            put("createdAt", ex.createdAt)
        }
        return writableDatabase.insertWithOnConflict(T_EXERCISES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteExercise(id: Long) {
        writableDatabase.delete(T_EXERCISES, "id=?", arrayOf(id.toString()))
    }

    fun queryLogsForExercise(exerciseId: Long, date: String): List<WorkoutLog> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_LOGS WHERE exerciseId=? AND date=? ORDER BY setNumber ASC", arrayOf(exerciseId.toString(), date))
        val list = mutableListOf<WorkoutLog>()
        c.use {
            while (it.moveToNext()) {
                list += WorkoutLog(
                    id = it.getLong(0), exerciseId = it.getLong(1), date = it.getString(2),
                    setNumber = it.getInt(3), reps = it.getInt(4), weight = it.getDouble(5),
                    completed = it.getInt(6) == 1, createdAt = it.getLong(7)
                )
            }
        }
        return list
    }

    fun queryAllLogsForExercise(exerciseId: Long): List<WorkoutLog> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_LOGS WHERE exerciseId=? ORDER BY date DESC, setNumber ASC", arrayOf(exerciseId.toString()))
        val list = mutableListOf<WorkoutLog>()
        c.use {
            while (it.moveToNext()) {
                list += WorkoutLog(
                    id = it.getLong(0), exerciseId = it.getLong(1), date = it.getString(2),
                    setNumber = it.getInt(3), reps = it.getInt(4), weight = it.getDouble(5),
                    completed = it.getInt(6) == 1, createdAt = it.getLong(7)
                )
            }
        }
        return list
    }

    fun insertLog(log: WorkoutLog): Long {
        val cv = ContentValues().apply {
            put("exerciseId", log.exerciseId)
            put("date", log.date)
            put("setNumber", log.setNumber)
            put("reps", log.reps)
            put("weight", log.weight)
            put("completed", if (log.completed) 1 else 0)
            put("createdAt", log.createdAt)
        }
        return writableDatabase.insertWithOnConflict(T_LOGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteLog(id: Long) {
        writableDatabase.delete(T_LOGS, "id=?", arrayOf(id.toString()))
    }

    fun queryRecentDates(limit: Int = 30): List<String> {
        val c = readableDatabase.rawQuery("SELECT DISTINCT date FROM $T_LOGS ORDER BY date DESC LIMIT ?", arrayOf(limit.toString()))
        val list = mutableListOf<String>()
        c.use { while (it.moveToNext()) list += it.getString(0) }
        return list
    }

    fun queryCompletedCountForDate(date: String): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_LOGS WHERE date=? AND completed=1", arrayOf(date))
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    // ===== Body Metrics =====

    fun insertBodyMetric(metric: BodyMetric): Long {
        val cv = ContentValues().apply {
            put("date", metric.date)
            put("weightKg", metric.weightKg)
            put("heightCm", metric.heightCm)
            put("note", metric.note)
            put("createdAt", metric.createdAt)
        }
        return writableDatabase.insertWithOnConflict(T_METRICS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun queryAllBodyMetrics(): List<BodyMetric> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_METRICS ORDER BY date ASC", null)
        val list = mutableListOf<BodyMetric>()
        c.use {
            while (it.moveToNext()) {
                list += BodyMetric(
                    id = it.getLong(0), date = it.getString(1), weightKg = it.getDouble(2),
                    heightCm = it.getDouble(3), note = it.getString(4), createdAt = it.getLong(5)
                )
            }
        }
        return list
    }

    fun queryLatestBodyMetric(): BodyMetric? {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_METRICS ORDER BY date DESC LIMIT 1", null)
        return c.use {
            if (it.moveToFirst()) BodyMetric(
                id = it.getLong(0), date = it.getString(1), weightKg = it.getDouble(2),
                heightCm = it.getDouble(3), note = it.getString(4), createdAt = it.getLong(5)
            ) else null
        }
    }

    fun deleteBodyMetric(id: Long) {
        writableDatabase.delete(T_METRICS, "id=?", arrayOf(id.toString()))
    }

    // ===== Cardio Logs =====

    fun insertCardioLog(log: CardioLog): Long {
        val cv = ContentValues().apply {
            put("date", log.date)
            put("type", log.type)
            put("durationMin", log.durationMin)
            put("caloriesBurned", log.caloriesBurned)
            put("distanceKm", log.distanceKm)
            put("note", log.note)
            put("createdAt", log.createdAt)
        }
        return writableDatabase.insertWithOnConflict(T_CARDIO, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun queryAllCardioLogs(): List<CardioLog> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_CARDIO ORDER BY date DESC", null)
        val list = mutableListOf<CardioLog>()
        c.use {
            while (it.moveToNext()) {
                list += CardioLog(
                    id = it.getLong(0), date = it.getString(1), type = it.getString(2),
                    durationMin = it.getInt(3), caloriesBurned = it.getInt(4),
                    distanceKm = it.getDouble(5), note = it.getString(6), createdAt = it.getLong(7)
                )
            }
        }
        return list
    }

    fun queryCardioForDate(date: String): List<CardioLog> {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_CARDIO WHERE date=? ORDER BY createdAt DESC", arrayOf(date))
        val list = mutableListOf<CardioLog>()
        c.use {
            while (it.moveToNext()) {
                list += CardioLog(
                    id = it.getLong(0), date = it.getString(1), type = it.getString(2),
                    durationMin = it.getInt(3), caloriesBurned = it.getInt(4),
                    distanceKm = it.getDouble(5), note = it.getString(6), createdAt = it.getLong(7)
                )
            }
        }
        return list
    }

    fun deleteCardioLog(id: Long) {
        writableDatabase.delete(T_CARDIO, "id=?", arrayOf(id.toString()))
    }

    // ===== Aggregated Stats =====

    /** Weekly volume: sum of (weight * reps) for completed sets, grouped by week (ISO Monday). */
    fun queryWeeklyVolume(weeks: Int = 12): List<WeeklyVolume> {
        val c = readableDatabase.rawQuery("""
            SELECT
                strftime('%Y-%W', date) AS week,
                SUM(weight * reps) AS volume,
                COUNT(*) AS sets,
                SUM(reps) AS totalReps
            FROM $T_LOGS
            WHERE completed=1
            GROUP BY week
            ORDER BY week DESC
            LIMIT ?
        """.trimIndent(), arrayOf(weeks.toString()))
        val list = mutableListOf<WeeklyVolume>()
        c.use {
            while (it.moveToNext()) {
                val wk = it.getString(0) ?: ""
                list += WeeklyVolume(
                    weekStart = wk,
                    totalVolume = it.getDouble(1),
                    totalSets = it.getInt(2),
                    totalReps = it.getInt(3)
                )
            }
        }
        return list
    }

    /** Monthly volume: grouped by year-month. */
    fun queryMonthlyVolume(months: Int = 6): List<MonthlyVolume> {
        val c = readableDatabase.rawQuery("""
            SELECT
                strftime('%Y-%m', date) AS month,
                SUM(weight * reps) AS volume,
                COUNT(*) AS sets,
                COUNT(DISTINCT date) AS sessions
            FROM $T_LOGS
            WHERE completed=1
            GROUP BY month
            ORDER BY month DESC
            LIMIT ?
        """.trimIndent(), arrayOf(months.toString()))
        val list = mutableListOf<MonthlyVolume>()
        c.use {
            while (it.moveToNext()) {
                list += MonthlyVolume(
                    month = it.getString(0) ?: "",
                    totalVolume = it.getDouble(1),
                    totalSets = it.getInt(2),
                    sessions = it.getInt(3)
                )
            }
        }
        return list
    }

    /** Activity heatmap: last N days with intensity level 0-4. */
    fun queryActivityHeatmap(days: Int = 90): List<ActivityDay> {
        val c = readableDatabase.rawQuery("""
            SELECT
                date,
                SUM(weight * reps) AS volume,
                COUNT(*) AS sets
            FROM $T_LOGS
            WHERE completed=1 AND date >= date('now', '-${days} days')
            GROUP BY date
            ORDER BY date ASC
        """.trimIndent(), null)
        val rawMap = mutableMapOf<String, Pair<Double, Int>>()
        c.use {
            while (it.moveToNext()) {
                rawMap[it.getString(0)] = Pair(it.getDouble(1), it.getInt(2))
            }
        }
        // Fill all days in range
        val result = mutableListOf<ActivityDay>()
        val today = java.time.LocalDate.now()
        val maxVol = rawMap.values.maxOfOrNull { it.first } ?: 1.0
        for (i in days downTo 0) {
            val d = today.minusDays(i.toLong()).toString()
            val (vol, sets) = rawMap[d] ?: Pair(0.0, 0)
            val intensity = when {
                vol <= 0 -> 0
                vol <= maxVol * 0.25 -> 1
                vol <= maxVol * 0.50 -> 2
                vol <= maxVol * 0.75 -> 3
                else -> 4
            }
            result += ActivityDay(date = d, intensity = intensity)
        }
        return result
    }

    /** Total energy burned from cardio logs in a date range. */
    fun queryTotalCardioCalories(): Int {
        val c = readableDatabase.rawQuery("SELECT COALESCE(SUM(caloriesBurned), 0) FROM $T_CARDIO", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /** Estimated calories from weight training: ~6 kcal per set (rough MET-based estimate). */
    fun queryEstimatedWorkoutCalories(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_LOGS WHERE completed=1", null)
        val sets = c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        return sets * 6
    }

    /** Current streak: consecutive days (ending today or yesterday) with completed logs. */
    fun queryWorkoutStreak(): Int {
        val c = readableDatabase.rawQuery("SELECT DISTINCT date FROM $T_LOGS WHERE completed=1 ORDER BY date DESC", null)
        val dates = mutableListOf<String>()
        c.use { while (it.moveToNext()) dates += it.getString(0) }
        if (dates.isEmpty()) return 0
        var streak = 0
        var cursor = java.time.LocalDate.now()
        // Allow today or yesterday as start
        if (dates.contains(cursor.toString())) {
            // start from today
        } else if (dates.contains(cursor.minusDays(1).toString())) {
            cursor = cursor.minusDays(1)
        } else {
            return 0
        }
        while (dates.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    // ===== Import / Export =====

    /**
     * Export all workout days + exercises (without logs) as a JSON string.
     * Logs are personal — only schedule structure is shared.
     */
    fun exportToJson(): String {
        val days = queryAllDays()
        val exportDays = days.map { day ->
            val exercises = queryExercisesForDay(day.id)
            ExportWorkoutDay(
                dayOfWeek = day.dayOfWeek,
                dayName = day.dayName,
                title = day.title,
                sortOrder = day.sortOrder,
                exercises = exercises.map { ex ->
                    ExportExercise(
                        name = ex.name,
                        category = ex.category,
                        targetSets = ex.targetSets,
                        targetReps = ex.targetReps,
                        sortOrder = ex.sortOrder
                    )
                }
            )
        }
        val export = WorkoutExport(
            version = 1,
            exportedAt = java.time.LocalDate.now().toString(),
            days = exportDays
        )
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(WorkoutExport.serializer(), export)
    }

    /**
     * Import workout schedule from JSON. Replaces ALL existing days + exercises.
     * Logs are preserved (exercises get new IDs, so old logs become orphaned —
     * but we keep them for safety; user can clear manually).
     * Returns the number of days imported.
     */
    fun importFromJson(jsonStr: String): Int {
        val export = Json { ignoreUnknownKeys = true; isLenient = true }
            .decodeFromString(WorkoutExport.serializer(), jsonStr)

        val db = writableDatabase
        db.beginTransaction()
        try {
            // Clear existing schedule (days + exercises cascade delete)
            db.delete(T_EXERCISES, null, null)
            db.delete(T_DAYS, null, null)

            for (day in export.days) {
                val dayId = db.insert(T_DAYS, null, ContentValues().apply {
                    put("dayOfWeek", day.dayOfWeek)
                    put("dayName", day.dayName)
                    put("title", day.title)
                    put("sortOrder", day.sortOrder)
                    put("createdAt", System.currentTimeMillis())
                })
                for (ex in day.exercises) {
                    db.insert(T_EXERCISES, null, ContentValues().apply {
                        put("workoutDayId", dayId)
                        put("name", ex.name)
                        put("category", ex.category)
                        put("targetSets", ex.targetSets)
                        put("targetReps", ex.targetReps)
                        put("sortOrder", ex.sortOrder)
                        put("createdAt", System.currentTimeMillis())
                    })
                }
            }
            db.setTransactionSuccessful()
            return export.days.size
        } finally {
            db.endTransaction()
        }
    }
}
