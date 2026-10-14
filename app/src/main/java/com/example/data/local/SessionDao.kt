package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Session
import com.example.data.model.SessionWithTournaments
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY dateEpochMs DESC, id DESC")
    fun getAllSessionsWithTournamentsFlow(): Flow<List<SessionWithTournaments>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionWithTournamentsById(id: Long): SessionWithTournaments?

    @Query("SELECT * FROM sessions ORDER BY dateEpochMs DESC, id DESC")
    fun getAllSessionsFlow(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY dateEpochMs DESC, id DESC")
    suspend fun getAllSessionsList(): List<Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<Session>): List<Long>

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("UPDATE tournaments SET sessionId = :sessionId WHERE id = :tournamentId")
    suspend fun linkTournamentToSession(tournamentId: Long, sessionId: Long?)

    @Query("UPDATE tournaments SET sessionId = NULL WHERE sessionId = :sessionId")
    suspend fun unlinkTournamentsFromSession(sessionId: Long)

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}
