package com.maya.callguardian.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CallHistoryEntity)

    @Query("SELECT * FROM call_history ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<CallHistoryEntity>>

    @Query("SELECT * FROM call_history WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): CallHistoryEntity?

    @Query("DELETE FROM call_history WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM call_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM call_history")
    suspend fun getSessionCount(): Int
}
