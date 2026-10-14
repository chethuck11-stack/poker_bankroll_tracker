package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "sessions",
    indices = [
        Index("dateEpochMs")
    ]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val dateEpochMs: Long,
    val startTime: String? = null,
    val endTime: String? = null,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class SessionWithTournaments(
    @Embedded
    val session: Session,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val tournaments: List<Tournament>
) {
    /**
     * Calculated dynamically from linked tournaments to prevent any data inconsistency.
     */
    val totalInvestmentPaise: Long
        get() = tournaments.sumOf { it.totalInvestmentPaise }

    val totalCashesPaise: Long
        get() = tournaments.sumOf { it.cashesPaise }

    val netProfitPaise: Long
        get() = tournaments.sumOf { it.netProfitPaise }

    val tournamentCount: Int
        get() = tournaments.size

    val roiPercentage: Double?
        get() {
            val invested = totalInvestmentPaise
            if (invested <= 0L) return null
            return (netProfitPaise.toDouble() / invested.toDouble()) * 100.0
        }
}
