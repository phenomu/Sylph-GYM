package com.phenom.gym.data.local.chat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatSession(
    val id: Long = 0,
    val title: String,
    val mode: String = "GYM",      // "GYM" | "CODE"
    val model: String = "glm-5.2",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ChatMessageRecord(
    val id: Long = 0,
    val sessionId: Long,
    val role: String,           // "user" | "assistant" | "system"
    val content: String,
    val isError: Boolean = false,
    val tokens: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class ChatDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "phenom_chat.db"
        private const val DB_VERSION = 1
        private const val T_SESSIONS = "chat_sessions"
        private const val T_MESSAGES = "chat_messages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $T_SESSIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                mode TEXT DEFAULT 'GYM',
                model TEXT DEFAULT 'glm-5.2',
                createdAt INTEGER DEFAULT 0,
                updatedAt INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sessionId INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                isError INTEGER DEFAULT 0,
                tokens INTEGER,
                createdAt INTEGER DEFAULT 0,
                FOREIGN KEY (sessionId) REFERENCES $T_SESSIONS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX idx_messages_session ON $T_MESSAGES(sessionId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $T_SESSIONS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ===== Sessions =====
    fun insertSession(session: ChatSession): Long {
        val cv = ContentValues().apply {
            put("title", session.title)
            put("mode", session.mode)
            put("model", session.model)
            put("createdAt", session.createdAt)
            put("updatedAt", session.updatedAt)
        }
        return writableDatabase.insertWithOnConflict(T_SESSIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateSessionTitle(id: Long, title: String) {
        writableDatabase.execSQL(
            "UPDATE $T_SESSIONS SET title=?, updatedAt=? WHERE id=?",
            arrayOf(title, System.currentTimeMillis(), id)
        )
    }

    fun updateSessionTimestamp(id: Long) {
        writableDatabase.execSQL(
            "UPDATE $T_SESSIONS SET updatedAt=? WHERE id=?",
            arrayOf(System.currentTimeMillis(), id)
        )
    }

    fun deleteSession(id: Long) {
        writableDatabase.delete(T_SESSIONS, "id=?", arrayOf(id.toString()))
    }

    fun queryAllSessions(): List<ChatSession> {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $T_SESSIONS ORDER BY updatedAt DESC", null
        )
        val list = mutableListOf<ChatSession>()
        c.use {
            while (it.moveToNext()) {
                list += ChatSession(
                    id = it.getLong(0),
                    title = it.getString(1),
                    mode = it.getString(2),
                    model = it.getString(3),
                    createdAt = it.getLong(4),
                    updatedAt = it.getLong(5)
                )
            }
        }
        return list
    }

    // ===== Messages =====
    fun insertMessage(msg: ChatMessageRecord): Long {
        val cv = ContentValues().apply {
            put("sessionId", msg.sessionId)
            put("role", msg.role)
            put("content", msg.content)
            put("isError", if (msg.isError) 1 else 0)
            put("tokens", msg.tokens)
            put("createdAt", msg.createdAt)
        }
        val id = writableDatabase.insertWithOnConflict(T_MESSAGES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        updateSessionTimestamp(msg.sessionId)
        return id
    }

    fun queryMessagesForSession(sessionId: Long): List<ChatMessageRecord> {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $T_MESSAGES WHERE sessionId=? ORDER BY createdAt ASC",
            arrayOf(sessionId.toString())
        )
        val list = mutableListOf<ChatMessageRecord>()
        c.use {
            while (it.moveToNext()) {
                list += ChatMessageRecord(
                    id = it.getLong(0),
                    sessionId = it.getLong(1),
                    role = it.getString(2),
                    content = it.getString(3),
                    isError = it.getInt(4) == 1,
                    tokens = if (it.isNull(5)) null else it.getInt(5),
                    createdAt = it.getLong(6)
                )
            }
        }
        return list
    }

    fun deleteAllMessagesForSession(sessionId: Long) {
        writableDatabase.delete(T_MESSAGES, "sessionId=?", arrayOf(sessionId.toString()))
    }
}
