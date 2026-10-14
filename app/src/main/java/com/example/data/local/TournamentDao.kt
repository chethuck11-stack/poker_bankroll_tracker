package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Tournament
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments ORDER BY dateEpochMs DESC, id DESC")
    fun getAllTournamentsFlow(): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments ORDER BY dateEpochMs DESC, id DESC")
    suspend fun getAllTournamentsList(): List<Tournament>

    @Query("SELECT * FROM tournaments WHERE id = :id LIMIT 1")
    suspend fun getTournamentById(id: Long): Tournament?

    @Query("SELECT * FROM tournaments WHERE sessionId = :sessionId ORDER BY dateEpochMs ASC")
    fun getTournamentsForSessionFlow(sessionId: Long): Flow<List<Tournament>>

    @Query("SELECT * FROM tournaments WHERE sessionId = :sessionId ORDER BY dateEpochMs ASC")
    suspend fun getTournamentsForSessionList(sessionId: Long): List<Tournament>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: Tournament): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTournaments(tournaments: List<Tournament>): List<Long>

    @Update
    suspend fun updateTournament(tournament: Tournament)

    @Delete
    suspend fun deleteTournament(tournament: Tournament)

    @Query("DELETE FROM tournaments WHERE id = :id")
    suspend fun deleteTournamentById(id: Long)

    @Query("DELETE FROM tournaments")
    suspend fun clearAll()
}
