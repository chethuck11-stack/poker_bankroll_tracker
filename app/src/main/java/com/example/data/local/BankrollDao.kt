package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankrollTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BankrollDao {

    @Query("SELECT * FROM bankroll_transactions ORDER BY dateEpochMs DESC, id DESC")
    fun getAllTransactionsFlow(): Flow<List<BankrollTransaction>>

    @Query("SELECT * FROM bankroll_transactions ORDER BY dateEpochMs ASC, id ASC")
    fun getAllTransactionsAscFlow(): Flow<List<BankrollTransaction>>

    @Query("SELECT * FROM bankroll_transactions ORDER BY dateEpochMs DESC, id DESC")
    suspend fun getAllTransactionsList(): List<BankrollTransaction>

    @Query("SELECT * FROM bankroll_transactions WHERE tournamentId = :tournamentId LIMIT 1")
    suspend fun getTransactionByTournamentId(tournamentId: Long): BankrollTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankrollTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<BankrollTransaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: BankrollTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: BankrollTransaction)

    @Query("DELETE FROM bankroll_transactions WHERE tournamentId = :tournamentId")
    suspend fun deleteByTournamentId(tournamentId: Long)

    @Query("DELETE FROM bankroll_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bankroll_transactions")
    suspend fun clearAll()
}
