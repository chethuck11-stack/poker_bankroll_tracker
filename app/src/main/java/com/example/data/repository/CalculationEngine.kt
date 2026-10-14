package com.example.data.repository

import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.Tournament
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class TournamentSummaryStats(
    val totalTournaments: Int = 0,
    val completedTournaments: Int = 0,
    val cancelledTournaments: Int = 0,
    val fullyRefundedTournaments: Int = 0,
    val totalInvestmentPaise: Long = 0L,
    val totalCashesPaise: Long = 0L,
    val netProfitPaise: Long = 0L,
    val roiPercentage: Double? = null,
    val itmPercentage: Double? = null,
    val averageBuyInPaise: Long = 0L,
    val averageProfitPaise: Long = 0L,
    val winningCount: Int = 0,
    val losingCount: Int = 0,
    val breakEvenCount: Int = 0,
    val bestCashPaise: Long = 0L,
    val largestLossPaise: Long = 0L
)

data class GroupProfitStat(
    val key: String,
    val tournamentsCount: Int,
    val totalInvestmentPaise: Long,
    val totalCashesPaise: Long,
    val netProfitPaise: Long,
    val roiPercentage: Double?
)

data class PeakDrawdownStats(
    val peakBankrollPaise: Long = 0L,
    val maxDrawdownPaise: Long = 0L,
    val maxDrawdownPercent: Double? = null
)

object CalculationEngine {

    /**
     * Computes poker tournament analytics adhering to strict financial rules.
     * Excludes cancelled and fully-refunded tournaments from completed stats, ITM and average buy-in.
     */
    fun computeStats(tournaments: List<Tournament>): TournamentSummaryStats {
        if (tournaments.isEmpty()) {
            return TournamentSummaryStats()
        }

        var completedCount = 0
        var cancelledCount = 0
        var fullyRefundedCount = 0
        var totalInvestmentPaise = 0L
        var totalCashesPaise = 0L
        var netProfitPaise = 0L
        var qualifyingCashCount = 0
        var winningCount = 0
        var losingCount = 0
        var breakEvenCount = 0
        var bestCashPaise = 0L
        var largestLossPaise = 0L

        for (t in tournaments) {
            when {
                t.status.name == "CANCELLED" -> {
                    cancelledCount++
                    // Excluded from played/completed stats. Still count any partial refund or fee net impact
                    netProfitPaise += t.netProfitPaise
                }
                t.status.name == "REFUNDED" && t.refundPaise >= t.totalInvestmentPaise && t.cashesPaise == 0L -> {
                    fullyRefundedCount++
                    netProfitPaise += t.netProfitPaise
                }
                else -> {
                    completedCount++
                    val invested = t.totalInvestmentPaise
                    val cashes = t.cashesPaise
                    val net = t.netProfitPaise

                    totalInvestmentPaise += invested
                    totalCashesPaise += cashes
                    netProfitPaise += net

                    if (cashes > 0L) {
                        qualifyingCashCount++
                    }
                    if (cashes > bestCashPaise) {
                        bestCashPaise = cashes
                    }

                    if (net > 0L) {
                        winningCount++
                    } else if (net < 0L) {
                        losingCount++
                        val lossAmount = -net
                        if (lossAmount > largestLossPaise) {
                            largestLossPaise = lossAmount
                        }
                    } else {
                        breakEvenCount++
                    }
                }
            }
        }

        val roi = if (totalInvestmentPaise > 0L) {
            (netProfitPaise.toDouble() / totalInvestmentPaise.toDouble()) * 100.0
        } else null

        val itm = if (completedCount > 0) {
            (qualifyingCashCount.toDouble() / completedCount.toDouble()) * 100.0
        } else null

        val avgBuyIn = if (completedCount > 0) {
            totalInvestmentPaise / completedCount
        } else 0L

        val avgProfit = if (completedCount > 0) {
            netProfitPaise / completedCount
        } else 0L

        return TournamentSummaryStats(
            totalTournaments = tournaments.size,
            completedTournaments = completedCount,
            cancelledTournaments = cancelledCount,
            fullyRefundedTournaments = fullyRefundedCount,
            totalInvestmentPaise = totalInvestmentPaise,
            totalCashesPaise = totalCashesPaise,
            netProfitPaise = netProfitPaise,
            roiPercentage = roi,
            itmPercentage = itm,
            averageBuyInPaise = avgBuyIn,
            averageProfitPaise = avgProfit,
            winningCount = winningCount,
            losingCount = losingCount,
            breakEvenCount = breakEvenCount,
            bestCashPaise = bestCashPaise,
            largestLossPaise = largestLossPaise
        )
    }

    /**
     * Calculates the current bankroll by summing net impacts of all ledger transactions.
     * Opening bankroll + deposits - withdrawals + net poker results + adjustments.
     */
    fun computeCurrentBankroll(transactions: List<BankrollTransaction>): Long {
        return transactions.sumOf { it.netImpactPaise }
    }

    /**
     * Calculates the pure poker profit across transactions.
     * Deposits, withdrawals, and initial balance do not affect poker profit.
     */
    fun computePokerProfitFromLedger(transactions: List<BankrollTransaction>): Long {
        return transactions.filter {
            it.type == BankrollTransactionType.TOURNAMENT_RESULT ||
                    it.type == BankrollTransactionType.BONUS_RAKEBACK ||
                    it.type == BankrollTransactionType.REFUND
        }.sumOf { it.netImpactPaise }
    }

    /**
     * Calculates running bankroll curve, peak bankroll, and maximum drawdown in Paise.
     */
    fun computePeakAndDrawdown(
        transactions: List<BankrollTransaction>
    ): PeakDrawdownStats {
        if (transactions.isEmpty()) return PeakDrawdownStats()

        val sorted = transactions.sortedBy { it.dateEpochMs }
        var current = 0L
        var peak = 0L
        var maxDrawdownPaise = 0L
        var peakAtMaxDrawdown = 0L

        for (tx in sorted) {
            current += tx.netImpactPaise
            if (current > peak) {
                peak = current
            }
            val drawdown = peak - current
            if (drawdown > maxDrawdownPaise) {
                maxDrawdownPaise = drawdown
                peakAtMaxDrawdown = peak
            }
        }

        val basePeak = if (peakAtMaxDrawdown > 0L) peakAtMaxDrawdown else peak
        val drawdownPercent = if (basePeak > 0L) {
            (maxDrawdownPaise.toDouble() / basePeak.toDouble()) * 100.0
        } else null

        return PeakDrawdownStats(
            peakBankrollPaise = max(peak, current),
            maxDrawdownPaise = maxDrawdownPaise,
            maxDrawdownPercent = drawdownPercent
        )
    }

    /**
     * Groups completed tournaments by Month (e.g. "Oct 2026", "Sep 2026").
     */
    fun groupProfitByMonth(tournaments: List<Tournament>): List<GroupProfitStat> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.US)
        val validTournaments = tournaments.filter { it.isCompleted }

        return validTournaments
            .groupBy { monthFormat.format(Date(it.dateEpochMs)) }
            .map { (monthStr, group) ->
                val invest = group.sumOf { it.totalInvestmentPaise }
                val cashes = group.sumOf { it.cashesPaise }
                val net = group.sumOf { it.netProfitPaise }
                val roi = if (invest > 0L) (net.toDouble() / invest.toDouble()) * 100.0 else null
                GroupProfitStat(
                    key = monthStr,
                    tournamentsCount = group.size,
                    totalInvestmentPaise = invest,
                    totalCashesPaise = cashes,
                    netProfitPaise = net,
                    roiPercentage = roi
                )
            }
    }

    /**
     * Groups completed tournaments by platform.
     */
    fun groupProfitByPlatform(tournaments: List<Tournament>): List<GroupProfitStat> {
        val validTournaments = tournaments.filter { it.isCompleted }
        return validTournaments
            .groupBy { it.platform.ifBlank { "Other" } }
            .map { (platform, group) ->
                val invest = group.sumOf { it.totalInvestmentPaise }
                val cashes = group.sumOf { it.cashesPaise }
                val net = group.sumOf { it.netProfitPaise }
                val roi = if (invest > 0L) (net.toDouble() / invest.toDouble()) * 100.0 else null
                GroupProfitStat(
                    key = platform,
                    tournamentsCount = group.size,
                    totalInvestmentPaise = invest,
                    totalCashesPaise = cashes,
                    netProfitPaise = net,
                    roiPercentage = roi
                )
            }
            .sortedByDescending { it.netProfitPaise }
    }

    /**
     * Groups completed tournaments by format (MTT, SNG, PKO, Satellite, etc.).
     */
    fun groupProfitByFormat(tournaments: List<Tournament>): List<GroupProfitStat> {
        val validTournaments = tournaments.filter { it.isCompleted }
        return validTournaments
            .groupBy { it.format.ifBlank { "MTT" } }
            .map { (fmt, group) ->
                val invest = group.sumOf { it.totalInvestmentPaise }
                val cashes = group.sumOf { it.cashesPaise }
                val net = group.sumOf { it.netProfitPaise }
                val roi = if (invest > 0L) (net.toDouble() / invest.toDouble()) * 100.0 else null
                GroupProfitStat(
                    key = fmt,
                    tournamentsCount = group.size,
                    totalInvestmentPaise = invest,
                    totalCashesPaise = cashes,
                    netProfitPaise = net,
                    roiPercentage = roi
                )
            }
            .sortedByDescending { it.netProfitPaise }
    }

    /**
     * Groups completed tournaments by Buy-In range:
     * - Micro: < ₹500
     * - Low: ₹500 - ₹1,999.99
     * - Mid: ₹2,000 - ₹4,999.99
     * - High: ≥ ₹5,000
     */
    fun groupProfitByBuyInTier(tournaments: List<Tournament>): List<GroupProfitStat> {
        val validTournaments = tournaments.filter { it.isCompleted }
        val tiers = listOf(
            "Micro (< ₹500)" to { t: Tournament -> t.initialBuyInPaise < 50_000L },
            "Low (₹500 - ₹1,999)" to { t: Tournament -> t.initialBuyInPaise in 50_000L..199_999L },
            "Mid (₹2,000 - ₹4,999)" to { t: Tournament -> t.initialBuyInPaise in 200_000L..499_999L },
            "High (₹5,000+)" to { t: Tournament -> t.initialBuyInPaise >= 500_000L }
        )

        return tiers.mapNotNull { (label, predicate) ->
            val group = validTournaments.filter(predicate)
            if (group.isEmpty()) null
            else {
                val invest = group.sumOf { it.totalInvestmentPaise }
                val cashes = group.sumOf { it.cashesPaise }
                val net = group.sumOf { it.netProfitPaise }
                val roi = if (invest > 0L) (net.toDouble() / invest.toDouble()) * 100.0 else null
                GroupProfitStat(
                    key = label,
                    tournamentsCount = group.size,
                    totalInvestmentPaise = invest,
                    totalCashesPaise = cashes,
                    netProfitPaise = net,
                    roiPercentage = roi
                )
            }
        }
    }
}
