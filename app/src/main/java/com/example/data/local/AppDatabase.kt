package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.Session
import com.example.data.model.Tournament
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Tournament::class,
        BankrollTransaction::class,
        Session::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tournamentDao(): TournamentDao
    abstract fun bankrollDao(): BankrollDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poker_bankroll_tracker.db"
                ).fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed starter data matching user's requested dashboard
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    val now = System.currentTimeMillis()
                                    // 1. Initial Bankroll ₹5,00,000 (50,000,000 paise)
                                    val initTx = BankrollTransaction(
                                        type = BankrollTransactionType.INITIAL_BANKROLL,
                                        amountPaise = 50_000_000L,
                                        dateEpochMs = now - (2 * 60 * 60 * 1000L),
                                        description = "Opening Bankroll"
                                    )
                                    database.bankrollDao().insertTransaction(initTx)

                                    // 2. Starter Tournament: Sunday Superstack
                                    val tournament = Tournament(
                                        name = "Sunday Superstack",
                                        dateEpochMs = now,
                                        platform = "Natural8 India / GGPoker",
                                        initialBuyInPaise = 300_000L, // ₹3,000
                                        cashesPaise = 865_000L, // ₹8,650 (Net win +₹5,650, ROI +188.3%)
                                        finishingPosition = 25,
                                        fieldSize = 800,
                                        format = TournamentFormat.MTT.displayName,
                                        status = TournamentStatus.COMPLETED
                                    )
                                    val tourneyId = database.tournamentDao().insertTournament(tournament)

                                    // 3. Linked tournament result transaction (+₹5,650)
                                    val tourneyTx = BankrollTransaction(
                                        type = BankrollTransactionType.TOURNAMENT_RESULT,
                                        amountPaise = 565_000L,
                                        dateEpochMs = now,
                                        description = "Tournament: Sunday Superstack (Natural8 India / GGPoker)",
                                        tournamentId = tourneyId
                                    )
                                    database.bankrollDao().insertTransaction(tourneyTx)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
