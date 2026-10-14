package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BackupData
import com.example.data.model.BackupValidationResult
import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.Session
import com.example.data.model.SessionWithTournaments
import com.example.data.model.Tournament
import com.example.data.repository.CalculationEngine
import com.example.data.repository.GroupProfitStat
import com.example.data.repository.PeakDrawdownStats
import com.example.data.repository.PokerRepository
import com.example.data.repository.TournamentSummaryStats
import com.example.ui.components.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilter(val displayName: String) {
    ALL_TIME("All Time"),
    CURRENT_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CURRENT_YEAR("This Year");
}

enum class ProfitFilter(val displayName: String) {
    ALL("All"),
    WINNING("Winning"),
    LOSING("Losing");
}

enum class TournamentSort(val displayName: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    PROFIT_DESC("Highest Profit"),
    BUYIN_DESC("Highest Buy-in");
}

data class TournamentFilterParams(
    val query: String,
    val platform: String?,
    val format: String?,
    val profit: ProfitFilter,
    val sort: TournamentSort
)

class PokerViewModel(private val repository: PokerRepository) : ViewModel() {

    val allTournaments: StateFlow<List<Tournament>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<BankrollTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessionsWithTournaments: StateFlow<List<SessionWithTournaments>> = repository.allSessionsWithTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<Session>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date Filter for Dashboard
    val selectedDateFilter = MutableStateFlow(DateFilter.ALL_TIME)

    // Lifetime Stats (All Time)
    val lifetimeStats: StateFlow<TournamentSummaryStats> = allTournaments.combine(MutableStateFlow(Unit)) { list, _ ->
        CalculationEngine.computeStats(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TournamentSummaryStats())

    // Filtered Dashboard Stats
    val dashboardStats: StateFlow<TournamentSummaryStats> = combine(
        allTournaments,
        selectedDateFilter
    ) { tournaments, filter ->
        val filtered = filterTournamentsByDate(tournaments, filter)
        CalculationEngine.computeStats(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TournamentSummaryStats())

    // Current Bankroll
    val currentBankroll: StateFlow<Long> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        CalculationEngine.computeCurrentBankroll(txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Opening Bankroll
    val openingBankroll: StateFlow<Long> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        txList.filter { it.type == BankrollTransactionType.INITIAL_BANKROLL }.sumOf { it.netImpactPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Net Transfers (Deposits - Withdrawals)
    val netTransfers: StateFlow<Long> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        txList.filter { it.type == BankrollTransactionType.DEPOSIT || it.type == BankrollTransactionType.WITHDRAWAL }
            .sumOf { it.netImpactPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Pure Poker Profit across bankroll ledger
    val lifetimePokerProfit: StateFlow<Long> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        CalculationEngine.computePokerProfitFromLedger(txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Peak & Drawdown
    val peakAndDrawdown: StateFlow<PeakDrawdownStats> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        CalculationEngine.computePeakAndDrawdown(txList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeakDrawdownStats())

    // Bankroll Growth Chart Points
    val bankrollChartPoints: StateFlow<List<ChartPoint>> = allTransactions.combine(MutableStateFlow(Unit)) { txList, _ ->
        val sorted = txList.sortedBy { it.dateEpochMs }
        val dateFormat = SimpleDateFormat("dd MMM", Locale.US)
        var running = 0L
        val points = mutableListOf<ChartPoint>()
        for (tx in sorted) {
            running += tx.netImpactPaise
            points.add(ChartPoint(label = dateFormat.format(Date(tx.dateEpochMs)), valuePaise = running, dateEpochMs = tx.dateEpochMs))
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Profit Stats for Chart
    val monthlyStats: StateFlow<List<GroupProfitStat>> = allTournaments.combine(MutableStateFlow(Unit)) { list, _ ->
        CalculationEngine.groupProfitByMonth(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Platform Grouping
    val platformStats: StateFlow<List<GroupProfitStat>> = allTournaments.combine(MutableStateFlow(Unit)) { list, _ ->
        CalculationEngine.groupProfitByPlatform(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Format Grouping
    val formatStats: StateFlow<List<GroupProfitStat>> = allTournaments.combine(MutableStateFlow(Unit)) { list, _ ->
        CalculationEngine.groupProfitByFormat(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Buy-in Tier Grouping
    val buyInTierStats: StateFlow<List<GroupProfitStat>> = allTournaments.combine(MutableStateFlow(Unit)) { list, _ ->
        CalculationEngine.groupProfitByBuyInTier(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filters for Tournaments List ---
    val searchQuery = MutableStateFlow("")
    val selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedFormatFilter = MutableStateFlow<String?>(null)
    val selectedProfitFilter = MutableStateFlow(ProfitFilter.ALL)
    val selectedSortOrder = MutableStateFlow(TournamentSort.DATE_DESC)
    private val filterParams = combine(
        combine(searchQuery, selectedPlatformFilter, selectedFormatFilter) { q, p, f -> Triple(q, p, f) },
        combine(selectedProfitFilter, selectedSortOrder) { pr, s -> Pair(pr, s) }
    ) { (q, p, f), (pr, s) ->
        TournamentFilterParams(q, p, f, pr, s)
    }

    val filteredTournaments: StateFlow<List<Tournament>> = combine(
        allTournaments,
        filterParams
    ) { tournaments, params ->
        var result = tournaments

        if (params.query.isNotBlank()) {
            val q = params.query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                        it.platform.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }

        if (!params.platform.isNullOrBlank()) {
            result = result.filter { it.platform.equals(params.platform, ignoreCase = true) }
        }

        if (!params.format.isNullOrBlank()) {
            result = result.filter { it.format.equals(params.format, ignoreCase = true) }
        }

        result = when (params.profit) {
            ProfitFilter.ALL -> result
            ProfitFilter.WINNING -> result.filter { it.netProfitPaise > 0L }
            ProfitFilter.LOSING -> result.filter { it.netProfitPaise < 0L }
        }

        when (params.sort) {
            TournamentSort.DATE_DESC -> result.sortedByDescending { it.dateEpochMs }
            TournamentSort.DATE_ASC -> result.sortedBy { it.dateEpochMs }
            TournamentSort.PROFIT_DESC -> result.sortedByDescending { it.netProfitPaise }
            TournamentSort.BUYIN_DESC -> result.sortedByDescending { it.totalInvestmentPaise }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun saveTournament(tournament: Tournament, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveTournament(tournament)
            onComplete?.invoke(id)
        }
    }

    fun deleteTournament(tournament: Tournament) {
        viewModelScope.launch {
            repository.deleteTournament(tournament)
        }
    }

    fun addBankrollTransaction(
        type: BankrollTransactionType,
        amountPaise: Long,
        description: String,
        dateEpochMs: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val tx = BankrollTransaction(
                type = type,
                amountPaise = amountPaise,
                dateEpochMs = dateEpochMs,
                description = description
            )
            repository.addBankrollTransaction(tx)
        }
    }

    fun deleteBankrollTransaction(transaction: BankrollTransaction) {
        viewModelScope.launch {
            repository.deleteBankrollTransaction(transaction)
        }
    }

    fun setInitialBankroll(amountPaise: Long) {
        viewModelScope.launch {
            repository.setInitialBankroll(amountPaise)
        }
    }

    fun saveSession(session: Session, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveSession(session)
            onComplete?.invoke(id)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun linkTournamentToSession(tournamentId: Long, sessionId: Long?) {
        viewModelScope.launch {
            repository.linkTournamentToSession(tournamentId, sessionId)
        }
    }

    suspend fun exportJson(): String = repository.exportDataToJson()
    suspend fun exportTournamentsCsv(): String = repository.exportTournamentsCsv()
    suspend fun exportTransactionsCsv(): String = repository.exportBankrollTransactionsCsv()
    suspend fun exportSessionsCsv(): String = repository.exportSessionsCsv()

    suspend fun validateBackupJson(json: String): Pair<BackupValidationResult, BackupData?> {
        return repository.validateBackupJson(json)
    }

    fun restoreBackup(data: BackupData, replaceExisting: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.restoreData(data, replaceExisting)
            onComplete()
        }
    }

    fun resetToDefaultData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetToDefaultData()
            onComplete()
        }
    }

    fun wipeCleanAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.wipeCleanAllData()
            onComplete()
        }
    }

    companion object {
        fun filterTournamentsByDate(tournaments: List<Tournament>, filter: DateFilter): List<Tournament> {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            return when (filter) {
                DateFilter.ALL_TIME -> tournaments
                DateFilter.LAST_30_DAYS -> {
                    val threshold = now - (30L * 24L * 60L * 60L * 1000L)
                    tournaments.filter { it.dateEpochMs >= threshold }
                }
                DateFilter.CURRENT_MONTH -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.timeInMillis
                    tournaments.filter { it.dateEpochMs >= startOfMonth }
                }
                DateFilter.CURRENT_YEAR -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfYear = calendar.timeInMillis
                    tournaments.filter { it.dateEpochMs >= startOfYear }
                }
            }
        }
    }
}

class PokerViewModelFactory(private val repository: PokerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PokerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PokerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
