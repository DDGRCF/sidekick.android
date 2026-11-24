package dev.ddgrcf.sidekick.core.store

import androidx.room3.Entity

@Entity(tableName = "sessions", primaryKeys = ["bridgeId", "sid"])
data class SessionEntity(
    val bridgeId: String,
    val sid: String,
    val tool: String,
    val cwd: String,
    val title: String?,
    val convId: String?,
    val live: Boolean,
    val lastActivityMs: Long,
    val updatedMs: Long,
)

@Entity(tableName = "messages", primaryKeys = ["bridgeId", "convId", "seq"])
data class MessageEntity(
    val bridgeId: String,
    val convId: String,
    val seq: Long,
    val role: String,
    val partsJson: String,
    val createdMs: Long,
    val model: String?,
)

@Entity(tableName = "history", primaryKeys = ["bridgeId", "convId"])
data class HistoryEntity(
    val bridgeId: String,
    val convId: String,
    val tool: String,
    val cwd: String,
    val title: String?,
    val preview: String?,
    val updatedMs: Long,
    val liveSid: String?,
)
