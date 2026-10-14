package com.example.data.model

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val tournamentCount: Int = 0,
    val transactionCount: Int = 0,
    val sessionCount: Int = 0,
    val duplicateTournamentsCount: Int = 0,
    val duplicateTransactionsCount: Int = 0
)

data class BackupData(
    val version: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val app: String = "Poker Bankroll Tracker",
    val tournaments: List<Tournament>,
    val transactions: List<BankrollTransaction>,
    val sessions: List<Session>
)
