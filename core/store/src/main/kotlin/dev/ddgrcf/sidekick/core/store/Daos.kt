package dev.ddgrcf.sidekick.core.store

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE bridgeId = :bridgeId ORDER BY live DESC, lastActivityMs DESC")
    fun observeByBridge(bridgeId: String): Flow<List<SessionEntity>>

    @Upsert
    suspend fun upsertAll(items: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE bridgeId = :bridgeId")
    suspend fun deleteByBridge(bridgeId: String)

    @Query("UPDATE sessions SET live = 0 WHERE bridgeId = :bridgeId AND sid = :sid")
    suspend fun markClosed(bridgeId: String, sid: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE bridgeId = :bridgeId AND convId = :convId ORDER BY seq ASC")
    fun observeMessages(bridgeId: String, convId: String): Flow<List<MessageEntity>>

    @Query("SELECT COALESCE(MAX(seq), 0) FROM messages WHERE bridgeId = :bridgeId AND convId = :convId")
    suspend fun maxSeq(bridgeId: String, convId: String): Long

    @Upsert
    suspend fun upsertAll(items: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE bridgeId = :bridgeId AND convId = :convId")
    suspend fun resetMessages(bridgeId: String, convId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE bridgeId = :bridgeId ORDER BY updatedMs DESC")
    fun observeByBridge(bridgeId: String): Flow<List<HistoryEntity>>

    @Upsert
    suspend fun upsertAll(items: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE bridgeId = :bridgeId")
    suspend fun deleteByBridge(bridgeId: String)
}
