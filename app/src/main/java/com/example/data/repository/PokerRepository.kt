package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.BackupData
import com.example.data.model.BackupValidationResult
import com.example.data.model.BankrollTransaction
import com.example.data.model.BankrollTransactionType
import com.example.data.model.CurrencyHelper
import com.example.data.model.Session
import com.example.data.model.SessionWithTournaments
import com.example.data.model.Tournament
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PokerRepository(private val db: AppDatabase) {

    private val tournamentDao = db.tournamentDao()
    private val bankrollDao = db.bankrollDao()
    private val sessionDao = db.sessionDao()

    val allTournaments: Flow<List<Tournament>> = tournamentDao.getAllTournamentsFlow()
    val allTransactions: Flow<List<BankrollTransaction>> = bankrollDao.getAllTransactionsFlow()
    val allSessionsWithTournaments: Flow<List<SessionWithTournaments>> = sessionDao.getAllSessionsWithTournamentsFlow()
    val allSessions: Flow<List<Session>> = sessionDao.getAllSessionsFlow()

    suspend fun saveTournament(tournament: Tournament): Long = withContext(Dispatchers.IO) {
        if (tournament.id == 0L) {
            val insertedId = tournamentDao.insertTournament(tournament)
            // Create linked bankroll transaction
            val transaction = BankrollTransaction(
                type = BankrollTransactionType.TOURNAMENT_RESULT,
                amountPaise = tournament.netProfitPaise,
                dateEpochMs = tournament.dateEpochMs,
                description = "Tournament: ${tournament.name} (${tournament.platform})",
                tournamentId = insertedId
            )
            bankrollDao.insertTransaction(transaction)
            insertedId
        } else {
            tournamentDao.updateTournament(tournament)
            val existingTx = bankrollDao.getTransactionByTournamentId(tournament.id)
            if (existingTx != null) {
                val updatedTx = existingTx.copy(
                    amountPaise = tournament.netProfitPaise,
                    dateEpochMs = tournament.dateEpochMs,
                    description = "Tournament: ${tournament.name} (${tournament.platform})"
                )
                bankrollDao.updateTransaction(updatedTx)
            } else {
                val transaction = BankrollTransaction(
                    type = BankrollTransactionType.TOURNAMENT_RESULT,
                    amountPaise = tournament.netProfitPaise,
                    dateEpochMs = tournament.dateEpochMs,
                    description = "Tournament: ${tournament.name} (${tournament.platform})",
                    tournamentId = tournament.id
                )
                bankrollDao.insertTransaction(transaction)
            }
            tournament.id
        }
    }

    suspend fun deleteTournament(tournament: Tournament) = withContext(Dispatchers.IO) {
        tournamentDao.deleteTournament(tournament)
        bankrollDao.deleteByTournamentId(tournament.id)
    }

    suspend fun addBankrollTransaction(transaction: BankrollTransaction): Long = withContext(Dispatchers.IO) {
        bankrollDao.insertTransaction(transaction)
    }

    suspend fun updateBankrollTransaction(transaction: BankrollTransaction) = withContext(Dispatchers.IO) {
        bankrollDao.updateTransaction(transaction)
    }

    suspend fun deleteBankrollTransaction(transaction: BankrollTransaction) = withContext(Dispatchers.IO) {
        bankrollDao.deleteTransaction(transaction)
    }

    suspend fun setInitialBankroll(amountPaise: Long, dateEpochMs: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val existingList = bankrollDao.getAllTransactionsList()
        val existingInitial = existingList.firstOrNull { it.type == BankrollTransactionType.INITIAL_BANKROLL }
        if (existingInitial != null) {
            val updated = existingInitial.copy(
                amountPaise = amountPaise,
                dateEpochMs = dateEpochMs,
                description = "Initial Bankroll"
            )
            bankrollDao.updateTransaction(updated)
        } else {
            val newInitial = BankrollTransaction(
                type = BankrollTransactionType.INITIAL_BANKROLL,
                amountPaise = amountPaise,
                dateEpochMs = dateEpochMs,
                description = "Initial Bankroll"
            )
            bankrollDao.insertTransaction(newInitial)
        }
    }

    suspend fun saveSession(session: Session): Long = withContext(Dispatchers.IO) {
        if (session.id == 0L) {
            sessionDao.insertSession(session)
        } else {
            sessionDao.updateSession(session)
            session.id
        }
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        sessionDao.unlinkTournamentsFromSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun linkTournamentToSession(tournamentId: Long, sessionId: Long?) = withContext(Dispatchers.IO) {
        sessionDao.linkTournamentToSession(tournamentId, sessionId)
    }

    // --- JSON Backup and Export ---

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val tournaments = tournamentDao.getAllTournamentsList()
        val transactions = bankrollDao.getAllTransactionsList()
        val sessions = sessionDao.getAllSessionsList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Poker Bankroll Tracker")
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        val tArray = JSONArray()
        for (t in tournaments) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("dateEpochMs", t.dateEpochMs)
            obj.put("platform", t.platform)
            obj.put("initialBuyInPaise", t.initialBuyInPaise)
            obj.put("format", t.format)
            obj.put("entryFeePaise", t.entryFeePaise)
            obj.put("reEntriesCount", t.reEntriesCount)
            obj.put("reEntryCostPaise", t.reEntryCostPaise)
            obj.put("addOnsCount", t.addOnsCount)
            obj.put("addOnCostPaise", t.addOnCostPaise)
            obj.put("additionalFeesPaise", t.additionalFeesPaise)
            obj.put("cashesPaise", t.cashesPaise)
            obj.put("refundPaise", t.refundPaise)
            obj.put("finishingPosition", t.finishingPosition ?: JSONObject.NULL)
            obj.put("fieldSize", t.fieldSize ?: JSONObject.NULL)
            obj.put("status", t.status.name)
            obj.put("notes", t.notes)
            obj.put("sessionId", t.sessionId ?: JSONObject.NULL)
            obj.put("createdAtEpochMs", t.createdAtEpochMs)
            tArray.put(obj)
        }
        root.put("tournaments", tArray)

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amountPaise", tx.amountPaise)
            obj.put("dateEpochMs", tx.dateEpochMs)
            obj.put("description", tx.description)
            obj.put("tournamentId", tx.tournamentId ?: JSONObject.NULL)
            obj.put("createdAtEpochMs", tx.createdAtEpochMs)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val sArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("dateEpochMs", s.dateEpochMs)
            obj.put("startTime", s.startTime ?: JSONObject.NULL)
            obj.put("endTime", s.endTime ?: JSONObject.NULL)
            obj.put("notes", s.notes)
            obj.put("createdAtEpochMs", s.createdAtEpochMs)
            sArray.put(obj)
        }
        root.put("sessions", sArray)

        root.toString(2)
    }

    suspend fun validateBackupJson(jsonString: String): Pair<BackupValidationResult, BackupData?> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("tournaments") && !root.has("transactions")) {
                return@withContext Pair(
                    BackupValidationResult(isValid = false, errorMessage = "File is not a valid Poker Bankroll Tracker backup."),
                    null
                )
            }

            val tArray = root.optJSONArray("tournaments") ?: JSONArray()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            val sArray = root.optJSONArray("sessions") ?: JSONArray()

            val existingTournaments = tournamentDao.getAllTournamentsList()
            val existingTx = bankrollDao.getAllTransactionsList()

            val parsedTournaments = mutableListOf<Tournament>()
            for (i in 0 until tArray.length()) {
                val obj = tArray.getJSONObject(i)
                parsedTournaments.add(
                    Tournament(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "Tournament"),
                        dateEpochMs = obj.optLong("dateEpochMs", System.currentTimeMillis()),
                        platform = obj.optString("platform", "Other"),
                        initialBuyInPaise = obj.optLong("initialBuyInPaise", 0L),
                        format = obj.optString("format", TournamentFormat.MTT.displayName),
                        entryFeePaise = obj.optLong("entryFeePaise", 0L),
                        reEntriesCount = obj.optInt("reEntriesCount", 0),
                        reEntryCostPaise = obj.optLong("reEntryCostPaise", 0L),
                        addOnsCount = obj.optInt("addOnsCount", 0),
                        addOnCostPaise = obj.optLong("addOnCostPaise", 0L),
                        additionalFeesPaise = obj.optLong("additionalFeesPaise", 0L),
                        cashesPaise = obj.optLong("cashesPaise", 0L),
                        refundPaise = obj.optLong("refundPaise", 0L),
                        finishingPosition = if (obj.isNull("finishingPosition")) null else obj.optInt("finishingPosition"),
                        fieldSize = if (obj.isNull("fieldSize")) null else obj.optInt("fieldSize"),
                        status = TournamentStatus.fromString(obj.optString("status", "COMPLETED")),
                        notes = obj.optString("notes", ""),
                        sessionId = if (obj.isNull("sessionId")) null else obj.optLong("sessionId"),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }

            val parsedTransactions = mutableListOf<BankrollTransaction>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                parsedTransactions.add(
                    BankrollTransaction(
                        id = obj.optLong("id", 0L),
                        type = BankrollTransactionType.fromString(obj.optString("type", "MANUAL_CORRECTION")),
                        amountPaise = obj.optLong("amountPaise", 0L),
                        dateEpochMs = obj.optLong("dateEpochMs", System.currentTimeMillis()),
                        description = obj.optString("description", ""),
                        tournamentId = if (obj.isNull("tournamentId")) null else obj.optLong("tournamentId"),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }

            val parsedSessions = mutableListOf<Session>()
            for (i in 0 until sArray.length()) {
                val obj = sArray.getJSONObject(i)
                parsedSessions.add(
                    Session(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "Session"),
                        dateEpochMs = obj.optLong("dateEpochMs", System.currentTimeMillis()),
                        startTime = if (obj.isNull("startTime")) null else obj.optString("startTime"),
                        endTime = if (obj.isNull("endTime")) null else obj.optString("endTime"),
                        notes = obj.optString("notes", ""),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    )
                )
            }

            // Duplicate detection (matching name, date, platform, initial buy in)
            val dupTCount = parsedTournaments.count { p ->
                existingTournaments.any { e ->
                    e.name == p.name && e.dateEpochMs == p.dateEpochMs && e.initialBuyInPaise == p.initialBuyInPaise
                }
            }

            val dupTxCount = parsedTransactions.count { p ->
                existingTx.any { e ->
                    e.type == p.type && e.dateEpochMs == p.dateEpochMs && e.amountPaise == p.amountPaise && e.description == p.description
                }
            }

            val data = BackupData(
                version = root.optInt("version", 1),
                exportedAtEpochMs = root.optLong("exportedAtEpochMs", System.currentTimeMillis()),
                app = root.optString("app", "Poker Bankroll Tracker"),
                tournaments = parsedTournaments,
                transactions = parsedTransactions,
                sessions = parsedSessions
            )

            Pair(
                BackupValidationResult(
                    isValid = true,
                    tournamentCount = parsedTournaments.size,
                    transactionCount = parsedTransactions.size,
                    sessionCount = parsedSessions.size,
                    duplicateTournamentsCount = dupTCount,
                    duplicateTransactionsCount = dupTxCount
                ),
                data
            )
        } catch (e: Exception) {
            Pair(
                BackupValidationResult(isValid = false, errorMessage = "Error parsing backup JSON: ${e.localizedMessage}"),
                null
            )
        }
    }

    suspend fun restoreData(data: BackupData, replaceExisting: Boolean) = withContext(Dispatchers.IO) {
        if (replaceExisting) {
            tournamentDao.clearAll()
            bankrollDao.clearAll()
            sessionDao.clearAll()

            sessionDao.insertAllSessions(data.sessions)
            tournamentDao.insertAllTournaments(data.tournaments)
            bankrollDao.insertAllTransactions(data.transactions)
        } else {
            // Merge strategy: insert new records, skip exact matches
            val existingTournaments = tournamentDao.getAllTournamentsList()
            val existingTx = bankrollDao.getAllTransactionsList()
            val existingSessions = sessionDao.getAllSessionsList()

            // Sessions
            val newSessions = data.sessions.filter { s ->
                existingSessions.none { it.name == s.name && it.dateEpochMs == s.dateEpochMs }
            }.map { it.copy(id = 0L) }
            sessionDao.insertAllSessions(newSessions)

            // Tournaments
            val newTournaments = data.tournaments.filter { t ->
                existingTournaments.none { it.name == t.name && it.dateEpochMs == t.dateEpochMs && it.initialBuyInPaise == t.initialBuyInPaise }
            }
            for (t in newTournaments) {
                saveTournament(t.copy(id = 0L))
            }

            // Non-tournament transactions
            val newTx = data.transactions.filter { tx ->
                tx.tournamentId == null && existingTx.none {
                    it.type == tx.type && it.dateEpochMs == tx.dateEpochMs && it.amountPaise == tx.amountPaise && it.description == tx.description
                }
            }.map { it.copy(id = 0L) }
            bankrollDao.insertAllTransactions(newTx)
        }
    }

    suspend fun resetToDefaultData() = withContext(Dispatchers.IO) {
        tournamentDao.clearAll()
        bankrollDao.clearAll()
        sessionDao.clearAll()

        val now = System.currentTimeMillis()
        val initTx = BankrollTransaction(
            type = BankrollTransactionType.INITIAL_BANKROLL,
            amountPaise = 50_000_000L,
            dateEpochMs = now - (2 * 60 * 60 * 1000L),
            description = "Opening Bankroll"
        )
        bankrollDao.insertTransaction(initTx)

        val tournament = Tournament(
            name = "Sunday Superstack",
            dateEpochMs = now,
            platform = "Natural8 India / GGPoker",
            initialBuyInPaise = 300_000L,
            cashesPaise = 865_000L,
            finishingPosition = 25,
            fieldSize = 800,
            format = TournamentFormat.MTT.displayName,
            status = TournamentStatus.COMPLETED
        )
        val tourneyId = tournamentDao.insertTournament(tournament)

        val tourneyTx = BankrollTransaction(
            type = BankrollTransactionType.TOURNAMENT_RESULT,
            amountPaise = 565_000L,
            dateEpochMs = now,
            description = "Tournament: Sunday Superstack (Natural8 India / GGPoker)",
            tournamentId = tourneyId
        )
        bankrollDao.insertTransaction(tourneyTx)
    }

    suspend fun wipeCleanAllData() = withContext(Dispatchers.IO) {
        tournamentDao.clearAll()
        bankrollDao.clearAll()
        sessionDao.clearAll()
    }

    // --- CSV Exports ---

    suspend fun exportTournamentsCsv(): String = withContext(Dispatchers.IO) {
        val list = tournamentDao.getAllTournamentsList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Date,Tournament Name,Platform,Format,Initial Buy-In (₹),Entry Fee (₹),Re-entries,Re-entry Cost (₹),Add-ons,Add-on Cost (₹),Additional Fees (₹),Total Investment (₹),Cashes (₹),Refund (₹),Net Profit (₹),ROI (%),Finish Position,Field Size,Status,Notes\n")

        for (t in list) {
            val dateStr = dateFormat.format(Date(t.dateEpochMs))
            val buyIn = CurrencyHelper.formatInr(t.initialBuyInPaise, includeSymbol = false)
            val entryFee = CurrencyHelper.formatInr(t.entryFeePaise, includeSymbol = false)
            val reEntryCost = CurrencyHelper.formatInr(t.reEntryCostPaise, includeSymbol = false)
            val addOnCost = CurrencyHelper.formatInr(t.addOnCostPaise, includeSymbol = false)
            val addFees = CurrencyHelper.formatInr(t.additionalFeesPaise, includeSymbol = false)
            val invest = CurrencyHelper.formatInr(t.totalInvestmentPaise, includeSymbol = false)
            val cashes = CurrencyHelper.formatInr(t.cashesPaise, includeSymbol = false)
            val refund = CurrencyHelper.formatInr(t.refundPaise, includeSymbol = false)
            val net = CurrencyHelper.formatInr(t.netProfitPaise, includeSymbol = false)
            val roi = t.roiPercentage?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A"

            sb.append("${t.id},\"${dateStr}\",\"${escapeCsv(t.name)}\",\"${escapeCsv(t.platform)}\",\"${t.format}\",\"$buyIn\",\"$entryFee\",${t.reEntriesCount},\"$reEntryCost\",${t.addOnsCount},\"$addOnCost\",\"$addFees\",\"$invest\",\"$cashes\",\"$refund\",\"$net\",\"$roi\",${t.finishingPosition ?: ""},${t.fieldSize ?: ""},\"${t.status.name}\",\"${escapeCsv(t.notes)}\"\n")
        }
        sb.toString()
    }

    suspend fun exportBankrollTransactionsCsv(): String = withContext(Dispatchers.IO) {
        val list = bankrollDao.getAllTransactionsList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Date,Transaction Type,Amount (₹),Net Impact (₹),Description,Tournament ID\n")

        for (tx in list) {
            val dateStr = dateFormat.format(Date(tx.dateEpochMs))
            val amount = CurrencyHelper.formatInr(tx.amountPaise, includeSymbol = false)
            val netImpact = CurrencyHelper.formatInr(tx.netImpactPaise, showSign = true, includeSymbol = false)
            sb.append("${tx.id},\"${dateStr}\",\"${tx.type.displayName}\",\"$amount\",\"$netImpact\",\"${escapeCsv(tx.description)}\",${tx.tournamentId ?: ""}\n")
        }
        sb.toString()
    }

    suspend fun exportSessionsCsv(): String = withContext(Dispatchers.IO) {
        val list = sessionDao.getAllSessionsList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Date,Session Name,Start Time,End Time,Notes\n")

        for (s in list) {
            val dateStr = dateFormat.format(Date(s.dateEpochMs))
            sb.append("${s.id},\"${dateStr}\",\"${escapeCsv(s.name)}\",\"${s.startTime ?: ""}\",\"${s.endTime ?: ""}\",\"${escapeCsv(s.notes)}\"\n")
        }
        sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
