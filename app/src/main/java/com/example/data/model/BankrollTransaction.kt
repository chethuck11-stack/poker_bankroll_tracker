package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.abs

enum class BankrollTransactionType(val displayName: String) {
    INITIAL_BANKROLL("Initial Bankroll"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TOURNAMENT_RESULT("Tournament Result"),
    REFUND("Refund"),
    BONUS_RAKEBACK("Bonus / Rakeback"),
    MANUAL_CORRECTION("Correction");

    companion object {
        fun fromString(value: String): BankrollTransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MANUAL_CORRECTION
        }
    }
}

@Entity(
    tableName = "bankroll_transactions",
    indices = [
        Index("dateEpochMs"),
        Index("tournamentId", unique = false)
    ]
)
data class BankrollTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: BankrollTransactionType,
    /**
     * Stored amount in Paise. For WITHDRAWAL, may be positive or negative;
     * [netImpactPaise] handles the exact sign. For TOURNAMENT_RESULT,
     * positive means net win, negative means net loss.
     */
    val amountPaise: Long,
    val dateEpochMs: Long,
    val description: String,
    val tournamentId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    /**
     * The exact mathematical impact on current bankroll in Paise.
     */
    val netImpactPaise: Long
        get() = when (type) {
            BankrollTransactionType.INITIAL_BANKROLL -> abs(amountPaise)
            BankrollTransactionType.DEPOSIT -> abs(amountPaise)
            BankrollTransactionType.WITHDRAWAL -> -abs(amountPaise)
            BankrollTransactionType.TOURNAMENT_RESULT -> amountPaise // Already signed (positive = win, negative = loss)
            BankrollTransactionType.REFUND -> abs(amountPaise)
            BankrollTransactionType.BONUS_RAKEBACK -> abs(amountPaise)
            BankrollTransactionType.MANUAL_CORRECTION -> amountPaise // Signed
        }
}
