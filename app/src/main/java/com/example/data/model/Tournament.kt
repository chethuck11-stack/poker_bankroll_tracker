package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tournaments",
    indices = [
        Index("dateEpochMs"),
        Index("platform"),
        Index("sessionId")
    ]
)
data class Tournament(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val dateEpochMs: Long,
    val platform: String,
    val initialBuyInPaise: Long,
    val format: String = TournamentFormat.MTT.displayName,
    val entryFeePaise: Long = 0L,
    val reEntriesCount: Int = 0,
    val reEntryCostPaise: Long = 0L,
    val addOnsCount: Int = 0,
    val addOnCostPaise: Long = 0L,
    val additionalFeesPaise: Long = 0L,
    val cashesPaise: Long = 0L,
    val refundPaise: Long = 0L,
    val finishingPosition: Int? = null,
    val fieldSize: Int? = null,
    val status: TournamentStatus = TournamentStatus.COMPLETED,
    val notes: String = "",
    val sessionId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    /**
     * Total money invested in this tournament in Paise:
     * Initial buy-in + (re-entries * cost) + (add-ons * cost) + entry fee + additional fees.
     */
    val totalInvestmentPaise: Long
        get() = initialBuyInPaise +
                (reEntriesCount.coerceAtLeast(0).toLong() * reEntryCostPaise.coerceAtLeast(0L)) +
                (addOnsCount.coerceAtLeast(0).toLong() * addOnCostPaise.coerceAtLeast(0L)) +
                entryFeePaise.coerceAtLeast(0L) +
                additionalFeesPaise.coerceAtLeast(0L)

    /**
     * Net profit in Paise:
     * Total cashes + applicable refunds - total investment.
     */
    val netProfitPaise: Long
        get() = cashesPaise + refundPaise - totalInvestmentPaise

    /**
     * Determines whether tournament counts as completed.
     * Cancelled tournaments and fully refunded tournaments do not count as completed tournaments.
     */
    val isCompleted: Boolean
        get() {
            if (status == TournamentStatus.CANCELLED) return false
            if (status == TournamentStatus.REFUNDED && refundPaise >= totalInvestmentPaise && cashesPaise == 0L) {
                return false
            }
            return true
        }

    /**
     * Qualifying cash: completed tournament with a positive cash outcome.
     */
    val isQualifyingCash: Boolean
        get() = isCompleted && cashesPaise > 0L

    /**
     * Tournament ROI %: (Net profit / Total Investment) * 100.
     * Returns null if total investment is zero.
     */
    val roiPercentage: Double?
        get() {
            val invested = totalInvestmentPaise
            if (invested <= 0L) return null
            return (netProfitPaise.toDouble() / invested.toDouble()) * 100.0
        }
}
