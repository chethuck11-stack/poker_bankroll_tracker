package com.example

import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.CurrencyHelper
import com.example.data.model.Tournament
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.data.repository.CalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PokerCalculationsTest {

    @Test
    fun testScenarioA() {
        // Scenario A:
        // - Five completed tournaments
        // - Total investment: ₹1,000 (100,000 paise)
        // - Total cashes: ₹1,400 (140,000 paise)
        // - Three tournaments with cashes
        // Expected: Profit: ₹400, ROI: 40%, ITM: 60%

        val tournaments = listOf(
            // T1: 200 buy-in, 600 cash -> Profit +400
            Tournament(
                id = 1L,
                name = "T1",
                dateEpochMs = 1000L,
                platform = "PokerBaazi",
                initialBuyInPaise = 20_000L,
                cashesPaise = 60_000L,
                status = TournamentStatus.COMPLETED
            ),
            // T2: 200 buy-in, 500 cash -> Profit +300
            Tournament(
                id = 2L,
                name = "T2",
                dateEpochMs = 2000L,
                platform = "PokerBaazi",
                initialBuyInPaise = 20_000L,
                cashesPaise = 50_000L,
                status = TournamentStatus.COMPLETED
            ),
            // T3: 200 buy-in, 300 cash -> Profit +100
            Tournament(
                id = 3L,
                name = "T3",
                dateEpochMs = 3000L,
                platform = "Spartan Poker",
                initialBuyInPaise = 20_000L,
                cashesPaise = 30_000L,
                status = TournamentStatus.COMPLETED
            ),
            // T4: 200 buy-in, 0 cash (uncashed) -> Loss -200
            Tournament(
                id = 4L,
                name = "T4",
                dateEpochMs = 4000L,
                platform = "Adda52",
                initialBuyInPaise = 20_000L,
                cashesPaise = 0L,
                status = TournamentStatus.COMPLETED
            ),
            // T5: 200 buy-in, 0 cash (uncashed) -> Loss -200
            Tournament(
                id = 5L,
                name = "T5",
                dateEpochMs = 5000L,
                platform = "Adda52",
                initialBuyInPaise = 20_000L,
                cashesPaise = 0L,
                status = TournamentStatus.COMPLETED
            )
        )

        val stats = CalculationEngine.computeStats(tournaments)

        assertEquals(5, stats.completedTournaments)
        assertEquals(100_000L, stats.totalInvestmentPaise) // ₹1,000
        assertEquals(140_000L, stats.totalCashesPaise) // ₹1,400
        assertEquals(40_000L, stats.netProfitPaise) // ₹400
        assertNotNull(stats.roiPercentage)
        assertEquals(40.0, stats.roiPercentage!!, 0.001) // 40%
        assertNotNull(stats.itmPercentage)
        assertEquals(60.0, stats.itmPercentage!!, 0.001) // 60%
    }

    @Test
    fun testScenarioB() {
        // Scenario B:
        // - Initial bankroll: ₹5,000 (500,000 paise)
        // - Deposit: ₹1,000 (100,000 paise)
        // - Tournament investment: ₹500 (50,000 paise)
        // - Tournament cashes: ₹800 (80,000 paise) -> net win +₹300 (30,000 paise)
        // - Withdrawal: ₹300 (30,000 paise)
        // Expected final bankroll: ₹6,000 (600,000 paise)
        // Expected net poker profit: ₹300 (30,000 paise)
        // Expected result: deposits and withdrawals must not change tournament ROI.

        val tournament = Tournament(
            id = 101L,
            name = "Tourney B",
            dateEpochMs = 3000L,
            platform = "PokerBaazi",
            initialBuyInPaise = 50_000L,
            cashesPaise = 80_000L,
            status = TournamentStatus.COMPLETED
        )

        val transactions = listOf(
            BankrollTransaction(
                id = 1L,
                type = BankrollTransactionType.INITIAL_BANKROLL,
                amountPaise = 500_000L, // ₹5,000
                dateEpochMs = 1000L,
                description = "Initial balance"
            ),
            BankrollTransaction(
                id = 2L,
                type = BankrollTransactionType.DEPOSIT,
                amountPaise = 100_000L, // ₹1,000
                dateEpochMs = 2000L,
                description = "UPI Deposit"
            ),
            BankrollTransaction(
                id = 3L,
                type = BankrollTransactionType.TOURNAMENT_RESULT,
                amountPaise = tournament.netProfitPaise, // +₹300
                dateEpochMs = 3000L,
                description = "Tournament win",
                tournamentId = tournament.id
            ),
            BankrollTransaction(
                id = 4L,
                type = BankrollTransactionType.WITHDRAWAL,
                amountPaise = 30_000L, // ₹300
                dateEpochMs = 4000L,
                description = "Cashout"
            )
        )

        val finalBankroll = CalculationEngine.computeCurrentBankroll(transactions)
        val pokerProfit = CalculationEngine.computePokerProfitFromLedger(transactions)
        val tourneyStats = CalculationEngine.computeStats(listOf(tournament))

        // Final bankroll: 5000 + 1000 + 300 - 300 = 6000
        assertEquals(600_000L, finalBankroll) // ₹6,000

        // Pure poker profit = 300
        assertEquals(30_000L, pokerProfit) // ₹300

        // Tournament ROI is 300 / 500 = 60%
        assertNotNull(tourneyStats.roiPercentage)
        assertEquals(60.0, tourneyStats.roiPercentage!!, 0.001)

        // Verify deposits/withdrawals didn't alter tournament ROI or tournament investment
        assertEquals(50_000L, tourneyStats.totalInvestmentPaise)
        assertEquals(30_000L, tourneyStats.netProfitPaise)
    }

    @Test
    fun testScenarioC() {
        // Scenario C:
        // - A tournament with multiple re-entries and add-ons
        // - An uncashed tournament
        // - A cancelled tournament
        // - A fully refunded tournament
        // - An edited tournament
        // - A deleted tournament

        // 1. Multiple re-entries and add-ons:
        // Buy-in 1000, 2 re-entries @ 1000 each, 1 add-on @ 500, entry fee 100
        // Total investment = 1000 + (2 * 1000) + (1 * 500) + 100 = 3600
        // Cash = 8000 -> net profit = 8000 - 3600 = 4400
        val tWithReentries = Tournament(
            id = 1L,
            name = "Re-entry Event",
            dateEpochMs = 1000L,
            platform = "Spartan",
            initialBuyInPaise = 100_000L, // ₹1000
            entryFeePaise = 10_000L, // ₹100
            reEntriesCount = 2,
            reEntryCostPaise = 100_000L, // 2 x ₹1000
            addOnsCount = 1,
            addOnCostPaise = 50_000L, // ₹500
            cashesPaise = 800_000L, // ₹8000
            status = TournamentStatus.COMPLETED
        )
        assertEquals(360_000L, tWithReentries.totalInvestmentPaise)
        assertEquals(440_000L, tWithReentries.netProfitPaise)

        // 2. Uncashed tournament:
        // Buy-in 500 -> net profit = -500
        val tUncashed = Tournament(
            id = 2L,
            name = "Uncashed",
            dateEpochMs = 2000L,
            platform = "PokerBaazi",
            initialBuyInPaise = 50_000L,
            cashesPaise = 0L,
            status = TournamentStatus.COMPLETED
        )
        assertEquals(50_000L, tUncashed.totalInvestmentPaise)
        assertEquals(-50_000L, tUncashed.netProfitPaise)

        // 3. Cancelled tournament:
        // Buy-in 500, status CANCELLED -> Should NOT count as completed
        val tCancelled = Tournament(
            id = 3L,
            name = "Cancelled Server Crash",
            dateEpochMs = 3000L,
            platform = "Adda52",
            initialBuyInPaise = 50_000L,
            refundPaise = 50_000L,
            status = TournamentStatus.CANCELLED
        )
        assertEquals(false, tCancelled.isCompleted)

        // 4. Fully refunded tournament:
        // Buy-in 1000, refund 1000, status REFUNDED -> Not completed
        val tRefunded = Tournament(
            id = 4L,
            name = "Overlay Void Refund",
            dateEpochMs = 4000L,
            platform = "Spartan",
            initialBuyInPaise = 100_000L,
            refundPaise = 100_000L,
            status = TournamentStatus.REFUNDED
        )
        assertEquals(false, tRefunded.isCompleted)

        // Compute stats for all 4
        val initialList = listOf(tWithReentries, tUncashed, tCancelled, tRefunded)
        val initialStats = CalculationEngine.computeStats(initialList)

        assertEquals(4, initialStats.totalTournaments)
        assertEquals(2, initialStats.completedTournaments) // only 1 and 2 are completed
        assertEquals(1, initialStats.cancelledTournaments)
        assertEquals(1, initialStats.fullyRefundedTournaments)

        // Total investment of completed: 3600 + 500 = 4100 (410,000 paise)
        assertEquals(410_000L, initialStats.totalInvestmentPaise)
        // Total cashes: 8000 (800,000 paise)
        assertEquals(800_000L, initialStats.totalCashesPaise)
        // Net profit: 4400 - 500 = 3900 (390,000 paise)
        assertEquals(390_000L, initialStats.netProfitPaise)

        // 5. Edited tournament: edit tUncashed to have a min-cash of ₹1000
        val tEdited = tUncashed.copy(cashesPaise = 100_000L) // +500 profit
        val editedList = listOf(tWithReentries, tEdited, tCancelled, tRefunded)
        val editedStats = CalculationEngine.computeStats(editedList)

        // Net profit now: 4400 + 500 = 4900 (490,000 paise)
        assertEquals(490_000L, editedStats.netProfitPaise)
        // ITM now: 2 out of 2 completed tournaments cashed = 100%
        assertNotNull(editedStats.itmPercentage)
        assertEquals(100.0, editedStats.itmPercentage!!, 0.001)

        // 6. Deleted tournament: delete tEdited
        val afterDeleteList = listOf(tWithReentries, tCancelled, tRefunded)
        val afterDeleteStats = CalculationEngine.computeStats(afterDeleteList)
        assertEquals(1, afterDeleteStats.completedTournaments)
        assertEquals(440_000L, afterDeleteStats.netProfitPaise)
    }

    @Test
    fun testCurrencyFormattingAndParsing() {
        assertEquals("₹1,250.00", CurrencyHelper.formatInr(125_000L))
        assertEquals("+₹400.00", CurrencyHelper.formatInr(40_000L, showSign = true))
        assertEquals("-₹250.00", CurrencyHelper.formatInr(-25_000L, showSign = true))
        assertEquals("₹1,250", CurrencyHelper.formatInrCompact(125_000L))

        // Parsing
        assertEquals(125_000L, CurrencyHelper.parseInrToPaise("1250"))
        assertEquals(125_050L, CurrencyHelper.parseInrToPaise("1250.50"))
        assertEquals(125_000L, CurrencyHelper.parseInrToPaise("₹ 1,250"))
        assertEquals(-50_000L, CurrencyHelper.parseInrToPaise("-500"))
        assertEquals(0L, CurrencyHelper.parseInrToPaise(""))
        assertEquals(0L, CurrencyHelper.parseInrToPaise("invalid"))

        // ROI formatting
        assertEquals("+40.0%", CurrencyHelper.formatRoi(40.0))
        assertEquals("-15.2%", CurrencyHelper.formatRoi(-15.2))
        assertEquals("N/A", CurrencyHelper.formatRoi(null))
        assertEquals("N/A", CurrencyHelper.formatRoi(Double.NaN))

        // ITM formatting
        assertEquals("60.0%", CurrencyHelper.formatItm(60.0))
        assertEquals("N/A", CurrencyHelper.formatItm(null))
    }

    @Test
    fun testEmptyStatesAndZeroDivisionSafety() {
        val emptyStats = CalculationEngine.computeStats(emptyList())
        assertEquals(0, emptyStats.totalTournaments)
        assertEquals(0, emptyStats.completedTournaments)
        assertEquals(0L, emptyStats.totalInvestmentPaise)
        assertEquals(0L, emptyStats.netProfitPaise)
        assertNull(emptyStats.roiPercentage)
        assertNull(emptyStats.itmPercentage)
        assertEquals(0L, emptyStats.averageBuyInPaise)
        assertEquals(0L, emptyStats.averageProfitPaise)

        // Zero bankroll transactions
        val zeroBankroll = CalculationEngine.computeCurrentBankroll(emptyList())
        assertEquals(0L, zeroBankroll)

        val emptyDrawdown = CalculationEngine.computePeakAndDrawdown(emptyList())
        assertEquals(0L, emptyDrawdown.peakBankrollPaise)
        assertEquals(0L, emptyDrawdown.maxDrawdownPaise)
        assertNull(emptyDrawdown.maxDrawdownPercent)
    }

    @Test
    fun testDrawdownCalculation() {
        // Transactions:
        // Day 1: +10,000 (Initial) -> Peak 10,000
        // Day 2: +5,000 (Win) -> Balance 15,000, Peak 15,000
        // Day 3: -3,000 (Loss) -> Balance 12,000, Drawdown 3,000
        // Day 4: -4,000 (Loss) -> Balance 8,000, Drawdown 7,000
        // Day 5: +10,000 (Win) -> Balance 18,000, Peak 18,000
        val txs = listOf(
            BankrollTransaction(id = 1L, type = BankrollTransactionType.INITIAL_BANKROLL, amountPaise = 10_000_00L, dateEpochMs = 1000L, description = "Init"),
            BankrollTransaction(id = 2L, type = BankrollTransactionType.TOURNAMENT_RESULT, amountPaise = 5_000_00L, dateEpochMs = 2000L, description = "Win"),
            BankrollTransaction(id = 3L, type = BankrollTransactionType.TOURNAMENT_RESULT, amountPaise = -3_000_00L, dateEpochMs = 3000L, description = "Loss"),
            BankrollTransaction(id = 4L, type = BankrollTransactionType.TOURNAMENT_RESULT, amountPaise = -4_000_00L, dateEpochMs = 4000L, description = "Loss"),
            BankrollTransaction(id = 5L, type = BankrollTransactionType.TOURNAMENT_RESULT, amountPaise = 10_000_00L, dateEpochMs = 5000L, description = "Big win")
        )

        val stats = CalculationEngine.computePeakAndDrawdown(txs)
        assertEquals(18_000_00L, stats.peakBankrollPaise) // Peak 18,000
        assertEquals(7_000_00L, stats.maxDrawdownPaise) // Max drawdown was 15,000 - 8,000 = 7,000
        assertNotNull(stats.maxDrawdownPercent)
        // 7,000 / 15,000 * 100 = 46.66%
        assertEquals(46.666, stats.maxDrawdownPercent!!, 0.1)
    }
}
